/*
 * Autopsy Forensic Browser
 * 
 * Copyright 2011 Basis Technology Corp.
 * Contact: carrier <at> sleuthkit <dot> org
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sleuthkit.autopsy.datamodel;

import org.sleuthkit.datamodel.AbstractFile;

/**
 * An abstract node that encapsulates AbstractFile data
 * @param <T> type of the AbstractFile to encapsulate
 * @param E type of object returned from visitor
 */
abstract class AbstractAbstractFileNode<T extends AbstractFile,E> extends AbstractContentNode<T,E> {
   
    /**
     *  @param <T> type of the AbstractFile data to encapsulate
     * @param abstractFile file to encapsulate
     */
    AbstractAbstractFileNode(T abstractFile) {
        super(abstractFile);
    }
    
}
