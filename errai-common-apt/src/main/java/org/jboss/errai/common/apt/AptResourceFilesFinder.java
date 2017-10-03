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

package org.jboss.errai.common.apt;

import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.File;
import java.io.IOException;
import java.net.URL;

/**
 * @author Tiago Bento <tfernand@redhat.com>
 */
public class AptResourceFilesFinder implements ResourceFilesFinder {
  private final Filer filer;

  public AptResourceFilesFinder(final Filer filer) {
    this.filer = filer;
  }

  @Override
  public URL getResource(final String path) {

    final int lastSlashIndex = path.lastIndexOf("/");
    final String packageName = path.substring(0, lastSlashIndex).replace("/", ".");
    final String fileName = path.substring(lastSlashIndex + 1);

    try {
      final FileObject resource = filer.getResource(StandardLocation.SOURCE_PATH, packageName, fileName);

      if (!new File(resource.toUri()).exists()) {
        return null;
      }

      return resource.toUri().toURL();

    } catch (IOException e) {
      return null;
    }
  }
}
