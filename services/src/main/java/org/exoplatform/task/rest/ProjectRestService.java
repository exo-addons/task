package org.exoplatform.task.rest;

import io.swagger.annotations.*;
import org.exoplatform.commons.utils.HTMLEntityEncoder;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.task.domain.Project;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.dto.StatusDto;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.exception.UnAuthorizedOperationException;
import org.exoplatform.task.legacy.service.UserService;
import org.exoplatform.task.model.User;
import org.exoplatform.task.service.*;
import org.exoplatform.task.util.ProjectUtil;
import org.exoplatform.task.util.StringUtil;
import org.exoplatform.task.util.UserUtil;
import org.gatein.common.text.EntityEncoder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;

@Path("/projects")
@Api(value = "/projects", description = "Managing tasks")
@RolesAllowed("users")
public class ProjectRestService implements ResourceContainer {

  private static final Log LOG           = ExoLogger.getLogger(ProjectRestService.class);

  private TaskService taskService;

  private ProjectService projectService;

  private StatusService statusService;

  private UserService userService;

  private SpaceService spaceService;

  private LabelService labelService;

  private CommentService commentService;

  public ProjectRestService(TaskService taskService,
                            CommentService commentService,
                            ProjectService projectService,
                            StatusService statusService,
                            UserService userService,
                            SpaceService spaceService,
                            LabelService labelService) {
    this.taskService = taskService;
    this.commentService = commentService;
    this.projectService = projectService;
    this.statusService = statusService;
    this.userService = userService;
    this.spaceService = spaceService;
    this.labelService = labelService;
  }

  private enum TaskType {
    ALL, INCOMING, OVERDUE
  }


  @GET
  @Path("projects")
  @RolesAllowed("users")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Gets projects", httpMethod = "GET", response = Response.class, notes = "This returns projects of the authenticated user")
  @ApiResponses(value = { @ApiResponse(code = 200, message = "Request fulfilled"),
      @ApiResponse(code = 500, message = "Internal server error") })
  public Response getProjects() {
    List<ProjectDto> projects = ProjectUtil.getProjectTree(null, projectService);
    JSONArray projectsJsonArray = new JSONArray();
    try {
      projectsJsonArray = buildJSON(projectsJsonArray, projects);
    } catch (Exception e) {
      LOG.error("Error getting projects", e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
    return Response.ok(projectsJsonArray.toString()).build();
  }

  @GET
  @Path("projects/status/{id}")
  @RolesAllowed("users")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Gets the default status by project id", httpMethod = "GET", response = Response.class, notes = "This returns the default status by project id")
  @ApiResponses(value = { @ApiResponse(code = 200, message = "Request fulfilled"),
      @ApiResponse(code = 403, message = "Unauthorized operation"), @ApiResponse(code = 500, message = "Internal server error") })
  public Response getDefaultStatusByProjectId(@ApiParam(value = "Project id", required = true) @PathParam("id") long id) throws EntityNotFoundException {
    Identity currentUser = ConversationState.getCurrent().getIdentity();
    ProjectDto project = projectService.getProject(id);
    if (project == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
    if (!project.canView(currentUser)) {
      return Response.status(Response.Status.UNAUTHORIZED).build();
    }
    StatusDto status = statusService.getDefaultStatus(id);
    return Response.ok(status).build();
  }

  @GET
  @Path("projects/statuses/{id}")
  @RolesAllowed("users")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Gets the statuses by project id", httpMethod = "GET", response = Response.class, notes = "This returns the statuses by project id")
  @ApiResponses(value = { @ApiResponse(code = 200, message = "Request fulfilled"),
      @ApiResponse(code = 403, message = "Unauthorized operation"), @ApiResponse(code = 404, message = "Resource not found") })
  public Response getStatusesByProjectId(@ApiParam(value = "Project id", required = true) @PathParam("id") long id) throws EntityNotFoundException {
    Identity currentUser = ConversationState.getCurrent().getIdentity();
    ProjectDto project = projectService.getProject(id);
    if (project == null) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }
    if (!project.canView(currentUser)) {
      return Response.status(Response.Status.UNAUTHORIZED).build();
    }
    List<StatusDto> projectStatuses = statusService.getStatuses(id);
    return Response.ok(projectStatuses).build();
  }

  @GET
  @Path("users/{query}/{projectName}")
  @RolesAllowed("users")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Gets users by query and project name", httpMethod = "GET", response = Response.class, notes = "This returns users by query and project name")
  @ApiResponses(value = { @ApiResponse(code = 200, message = "Request fulfilled") })
  public Response getUsersByQueryAndProjectName(@ApiParam(value = "Query", required = true) @PathParam("query") String query,
                                                @ApiParam(value = "projectName", required = true) @PathParam("projectName") String projectName) throws Exception {
    ListAccess<User> usersList = userService.findUserByName(query);
    JSONArray usersJsonArray = new JSONArray();
    for (User user : usersList.load(0, UserUtil.SEARCH_LIMIT)) {
      JSONObject userJson = new JSONObject();
      Space space = spaceService.getSpaceByPrettyName(projectName);
      if (space == null || spaceService.isMember(space, user.getUsername())) {
        userJson.put("username", user.getUsername());
        userJson.put("fullname", user.getDisplayName());
        userJson.put("avatar", user.getAvatar());
        usersJsonArray.put(userJson);
      }
    }
    return Response.ok(usersJsonArray.toString()).build();
  }


  private JSONArray buildJSON(JSONArray projectsJsonArray, List<ProjectDto> projects) throws JSONException {
    Identity currentUser = ConversationState.getCurrent().getIdentity();
    for (ProjectDto project : projects) {
      if (project.canView(currentUser)) {
        long projectId = project.getId();
        JSONObject projectJson = new JSONObject();
        List<Object[]> statusObjects = taskService.countTaskStatusByProject(projectId);
        JSONArray statusStats = new JSONArray();
        if (statusObjects != null && statusObjects.size() > 0) {

          for (Object[] result : statusObjects) {
            JSONObject statJson = new JSONObject();
            statJson.put("status", (String) result[0]);
            statJson.put("taskNumber", ((Number) result[1]).intValue());
            statusStats.put(statJson);
          }

        }
        projectJson.put("statusStats", statusStats);
        Space space = null;
        Set<String> projectManagers = projectService.getManager(projectId);
        Set<String> managers = new LinkedHashSet();
        if (projectManagers.size() > 0) {
          for (String permission : projectService.getManager(projectId)) {
            int index = permission.indexOf(':');
            if (index > -1) {
              String groupId = permission.substring(index + 1);
              space = spaceService.getSpaceByGroupId(groupId);
              managers.addAll(Arrays.asList(space.getManagers()));
            } else {
              managers.add(permission);
            }
          }
        }
        if (managers.size() > 0) {
          JSONArray managersJsonArray = new JSONArray();
          for (String usr : managers) {
            JSONObject manager = new JSONObject();
            User user_ = UserUtil.getUser(usr);
            manager.put("username", user_.getUsername());
            manager.put("email", user_.getEmail());
            manager.put("displayName", user_.getDisplayName());
            manager.put("avatar", user_.getAvatar());
            manager.put("url", user_.getUrl());
            manager.put("enable", user_.isEnable());
            manager.put("deleted", user_.isDeleted());
            managersJsonArray.put(manager);
          }
          projectJson.put("managerIdentities", managersJsonArray);
        }
        if (space != null) {
          JSONObject spaceJson = new JSONObject();
          spaceJson.put("prettyName", space.getPrettyName());
          spaceJson.put("url", space.getUrl());
          spaceJson.put("displayName", space.getDisplayName());
          spaceJson.put("id", space.getId());
          spaceJson.put("avatarUrl", space.getAvatarUrl());
          spaceJson.put("description", space.getDescription());
          projectJson.put("space", space);
        }

        projectJson.put("id", projectId);
        projectJson.put("name", project.getName());
        projectJson.put("color", project.getColor());
        projectJson.put("participator", projectService.getParticipator(projectId));
        projectJson.put("hiddenOn", project.getHiddenOn());
        projectJson.put("manager", projectService.getManager(projectId));
        projectJson.put("children", projectService.getSubProjects(projectId, 0, -1));
        projectJson.put("dueDate", project.getDueDate());
        projectJson.put("calendarIntegrated", project.isCalendarIntegrated());
        projectJson.put("description", project.getDescription());
        projectJson.put("status", statusService.getStatus(projectId));
        projectsJsonArray.put(projectJson);
        if (projectService.getSubProjects(projectId, 0, -1) != null) {
          List<ProjectDto> children = projectService.getSubProjects(projectId, 0, -1);
          buildJSON(projectsJsonArray, children);
        }
      }
    }
    return projectsJsonArray;
  }


  private Space getProjectSpace(ProjectDto project, SpaceService spaceService) {
    for (String permission : projectService.getManager(project.getId())) {
      int index = permission.indexOf(':');
      if (index > -1) {
        String groupId = permission.substring(index + 1);
        Space space = spaceService.getSpaceByGroupId(groupId);
        return space;

      }
    }

    return null;
  }

  @POST
  @Path("createproject")
  @RolesAllowed("users")
  @ApiOperation(value = "Adds a project", httpMethod = "POST", response = Response.class, notes = "This Adds project")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "Request fulfilled"),
          @ApiResponse(code = 400, message = "Invalid query input"),
          @ApiResponse(code = 403, message = "Unauthorized operation"),
          @ApiResponse(code = 404, message = "Resource not found")})
  public Response createProject(@ApiParam(value = "ProjectDto", required = true) ProjectDto projectDto) throws EntityNotFoundException, JSONException, UnAuthorizedOperationException {

    String currentUser = ConversationState.getCurrent().getIdentity().getUserId();
    if (currentUser == null) {
      return Response.status(Response.Status.FORBIDDEN).build();
    }

    if (projectDto.getName() == null || projectDto.getName().isEmpty()) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }


    String description = StringUtil.encodeInjectedHtmlTag(projectDto.getDescription());

    ProjectDto project;
    Space space = getProjectSpace(projectDto, spaceService);
    if (space != null) {
      List<String> memberships = UserUtil.getSpaceMemberships(space.getId());
      Set<String> managers = new HashSet<String>(Arrays.asList(currentUser, memberships.get(0)));
      Set<String> participators = new HashSet<String>(Arrays.asList(memberships.get(1)));
      project = ProjectUtil.newProjectInstanceDto(projectDto.getName(), description, managers, participators);
    } else {
      project = ProjectUtil.newProjectInstanceDto(projectDto.getName(), description, currentUser);
    }
    boolean calInteg = projectDto.isCalendarIntegrated();
    calInteg = projectDto.isCalendarIntegrated() ? false : calInteg;
    project.setCalendarIntegrated(calInteg);
    if (projectDto.getParent() != null) {
      Long parentId = projectDto.getParent().getId();
      ProjectDto parent = projectService.getProject(parentId);
      if (!parent.canEdit(ConversationState.getCurrent().getIdentity())) {
        throw new UnAuthorizedOperationException(parentId, Project.class, "getNoPermissionMsg()");
      }
      project = projectService.createProject(project, parentId);
    } else {
      project = projectService.createProject(project);
      statusService.createInitialStatuses(project);
    }

    EntityEncoder encoder = HTMLEntityEncoder.getInstance();
    JSONObject result = new JSONObject();
    result.put("id", project.getId());
    result.put("name", encoder.encode(project.getName()));
    result.put("color", "transparent");

    return Response.ok(result.toString()).build();
  }

}