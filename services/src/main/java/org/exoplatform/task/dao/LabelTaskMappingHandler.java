/* 
* Copyright (C) 2003-2015 eXo Platform SAS.
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Affero General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Affero General Public License for more details.
*
* You should have received a copy of the GNU Affero General Public License
* along with this program. If not, see http://www.gnu.org/licenses/ .
*/
package org.exoplatform.task.dao;

import java.io.Serializable;
import java.util.List;

import org.exoplatform.commons.api.persistence.GenericDAO;
import org.exoplatform.task.domain.LabelTaskMapping;

public interface LabelTaskMappingHandler extends GenericDAO<LabelTaskMapping, Serializable> {
    LabelTaskMapping findLabelTaskMapping(long labelId, long taskId);
    List<LabelTaskMapping> findLabelMappingByLabel(long labelId);
}

