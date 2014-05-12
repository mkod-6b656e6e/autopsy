/*
 * Autopsy Forensic Browser
 *
 * Copyright 2011-2014 Basis Technology Corp.
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

import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children.Keys;
import org.openide.nodes.Node;
import org.openide.util.NbBundle;
import org.sleuthkit.datamodel.Content;
import org.sleuthkit.datamodel.DerivedFile;
import org.sleuthkit.datamodel.Directory;
import org.sleuthkit.datamodel.File;
import org.sleuthkit.datamodel.Image;
import org.sleuthkit.datamodel.LayoutFile;
import org.sleuthkit.datamodel.LocalFile;
import org.sleuthkit.datamodel.SleuthkitItemVisitor;
import org.sleuthkit.datamodel.SleuthkitVisitableItem;
import org.sleuthkit.datamodel.TskException;
import org.sleuthkit.datamodel.VirtualDirectory;
import org.sleuthkit.datamodel.Volume;

/**
 * Abstract subclass for ContentChildren and RootContentChildren implementations
 * that handles creating Nodes from Content objects.
 */
abstract class AbstractContentChildren<T> extends Keys<T> {

    private final CreateSleuthkitNodeVisitor createSleuthkitNodeVisitor = new CreateSleuthkitNodeVisitor();
    private final CreateAutopsyNodeVisitor createAutopsyNodeVisitor = new CreateAutopsyNodeVisitor();

    /**
     * Uses lazy Content.Keys
     */
    AbstractContentChildren() {
        super(true); // use lazy behavior
    }

    @Override
    protected Node[] createNodes(T key) {
        if (key instanceof SleuthkitVisitableItem) {
            return new Node[]{((SleuthkitVisitableItem) key).accept(createSleuthkitNodeVisitor)};
        } else {
            return new Node[]{((AutopsyVisitableItem) key).accept(createAutopsyNodeVisitor)};
        }
    }

    /**
     * Creates appropriate Node for each sub-class of Content
     */
    public static class CreateSleuthkitNodeVisitor extends SleuthkitItemVisitor.Default<AbstractContentNode<? extends Content>> {

        @Override
        public AbstractContentNode<? extends Content> visit(Directory drctr) {
            return new DirectoryNode(drctr);
        }

        @Override
        public AbstractContentNode<? extends Content> visit(File file) {
            return new FileNode(file);
        }

        @Override
        public AbstractContentNode<? extends Content> visit(Image image) {
            return new ImageNode(image);
        }

        @Override
        public AbstractContentNode<? extends Content> visit(Volume volume) {
            return new VolumeNode(volume);
        }

        @Override
        public AbstractContentNode<? extends Content> visit(LayoutFile lf) {
            return new LayoutFileNode(lf);
        }

        @Override
        public AbstractContentNode<? extends Content> visit(DerivedFile df) {
            return new LocalFileNode(df);
        }

        @Override
        public AbstractContentNode<? extends Content> visit(LocalFile lf) {
            return new LocalFileNode(lf);
        }

        @Override
        public AbstractContentNode<? extends Content> visit(VirtualDirectory ld) {
            return new VirtualDirectoryNode(ld);
        }

        @Override
        protected AbstractContentNode<? extends Content> defaultVisit(SleuthkitVisitableItem di) {
            throw new UnsupportedOperationException(NbBundle.getMessage(this.getClass(),
                    "AbstractContentChildren.CreateTSKNodeVisitor.exception.noNodeMsg"));
        }
    }

    /**
     * Creates appropriate Node for each supported artifact category / grouping
     */
    static class CreateAutopsyNodeVisitor extends AutopsyItemVisitor.Default<AbstractNode> {

        @Override
        public ExtractedContent.RootNode visit(ExtractedContent ec) {
            return ec.new RootNode(ec.getSleuthkitCase());
        }

        @Override
        public AbstractNode visit(FileTypeExtensionFilters sf) {
            return new FileTypesNode(sf.getSleuthkitCase(), null);
        }

        @Override
        public AbstractNode visit(RecentFiles rf) {
            return new RecentFilesNode(rf.getSleuthkitCase());
        }

        @Override
        public AbstractNode visit(DeletedContent dc) {
            return new DeletedContent.DeletedContentsNode(dc.getSleuthkitCase());
        }

        @Override
        public AbstractNode visit(FileSize dc) {
            return new FileSize.FileSizeRootNode(dc.getSleuthkitCase());
        }

        @Override
        public AbstractNode visit(KeywordHits kh) {
            return kh.new RootNode();
        }

        @Override
        public AbstractNode visit(HashsetHits hh) {
            return hh.new RootNode();
        }

        @Override
        public AbstractNode visit(InterestingHits ih) {
            return ih.new RootNode();
        }

        @Override
        public AbstractNode visit(EmailExtracted ee) {
            return ee.new RootNode();
        }

        @Override
        public AbstractNode visit(Tags tagsNodeKey) {
            return tagsNodeKey.new RootNode();
        }

        @Override
        public AbstractNode visit(DataSources i) {
            try {
                return new DataSourcesNode(i.getSleuthkitCase().getRootObjects());
            } catch (TskException ex) {
                return defaultVisit(i);
            }
        }

        @Override
        public AbstractNode visit(Views v) {
            return new ViewsNode(v.getSleuthkitCase());
        }

        @Override
        public AbstractNode visit(Results r) {
            return new ResultsNode(r.getSleuthkitCase());
        }

        @Override
        protected AbstractNode defaultVisit(AutopsyVisitableItem di) {
            throw new UnsupportedOperationException(
                    NbBundle.getMessage(this.getClass(),
                    "AbstractContentChildren.createAutopsyNodeVisitor.exception.noNodeMsg"));
        }
    }
}
