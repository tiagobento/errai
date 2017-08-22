/*
 * Copyright (C) 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.common.apt.exportfile;

import org.apache.commons.lang3.RandomStringUtils;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

/**
 * @author Tiago Bento <tfernand@redhat.com>
 */
public class ExportFileName {

  private static final String ERRAI_MODULE_EXPORT_FILE_NAME_PREFIX = "ErraiModuleExportFile_";

  public static String buildExportFileNameForAnnotation(final TypeElement annotation) {
    String annotationName = annotation.getQualifiedName().toString().replace(".", "_");
    return RandomStringUtils.randomAlphabetic(4) + "_" + ERRAI_MODULE_EXPORT_FILE_NAME_PREFIX + annotationName;
  }

  public static String getAnnotationClassNameFromExportFileName(final Element e) {
    final String exportFileName = e.asType().toString();
    return exportFileName.substring(getAnnotationNameBeginIndex(exportFileName)).replace("_", ".");
  }

  private static int getAnnotationNameBeginIndex(final String exportFileName) {
    return exportFileName.indexOf(ERRAI_MODULE_EXPORT_FILE_NAME_PREFIX) + ERRAI_MODULE_EXPORT_FILE_NAME_PREFIX.length();
  }
}
