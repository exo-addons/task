<template>
  <div :id="status.id">
    <div v-if="filterByStatus===true">
      <tasks-view-header-status
        :status="status"
        :project="project"
        :view-type="'list'"
        :max-tasks-to-show="maxTasksToShow"
        :tasks-number="tasksList.length" />
    </div>
    <div :id="'taskView'+status.id" :class="filterByStatus===true ? 'pt-5 ms-7 me-2' : ''">
      <div :id="status.id">
        <draggable
          v-model="tasksList"
          :move="checkMove"
          :animation="200"
          group="people"
          class="draggable-palceholder"
          ghost-class="ghost-card"
          @start="drag=true"
          @end="drag=false">
          <task-view-list-item
            v-for="tasks in tasksList"
            :key="tasks.task.id"
            :task="tasks"
            :show-completed-tasks="showCompletedTasks"
            @update-task-completed="updateTaskCompleted" />
        </draggable>
      </div>
    </div>
  </div>
</template>
<script>

 
export default {
  props: {
    tasksList: {
      type: Array,
      default: () => []
    },
    status: {
      type: String,
      default: ''
    },
    showCompletedTasks: {
      type: Boolean,
      default: false
    },
    project: {
      type: Number,
      default: 0
    },
    filterByStatus: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      maxTasksToShow: 6,
      drag: false,
      task: null,
      newStatus: null,
    };
  },
  watch: {
    drag(val) {
      if (!val&&this.task&&this.newStatus&&this.task.status.id.toString() !== this.newStatus){
        this.$emit('updateTaskStatus', this.task,this.newStatus);
        Array.from(document.getElementsByClassName('draggable-palceholder')).forEach(element => element.style.backgroundColor= '#FFFFFF');
      }
    },
  },
  methods: {
    updateTaskCompleted(e){
      this.$emit('updateTaskCompleted', e);
    },
    checkMove(evt){
      if (evt){
        Array.from(document.getElementsByClassName('draggable-palceholder')).forEach(element => element.style.backgroundColor= '#FFFFFF');
        Array.from(evt.to.parentElement.getElementsByClassName('draggable-palceholder')).forEach(element => element.style.backgroundColor= '#f2f2f2');
        this.task = evt.draggedContext.element.task;
        this.newStatus = evt.to.parentElement.id;
      }

    },
  }
};
</script>
