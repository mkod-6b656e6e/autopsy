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


import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.logging.Level;

import org.openide.util.NbBundle;
import org.sleuthkit.autopsy.coreutils.Logger;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.Sheet;
import org.openide.util.Exceptions;
import org.openide.util.lookup.Lookups;
import org.sleuthkit.autopsy.ingest.IngestManager;
import org.sleuthkit.autopsy.ingest.ModuleDataEvent;
import org.sleuthkit.datamodel.BlackboardArtifact;
import org.sleuthkit.datamodel.BlackboardAttribute;
import org.sleuthkit.datamodel.SleuthkitCase;
import org.sleuthkit.datamodel.TskCoreException;


public class InterestingHits implements AutopsyVisitableItem {
    
    private static final String INTERESTING_ITEMS = NbBundle
            .getMessage(InterestingHits.class, "InterestingHits.interestingItems.text");
    private static final String DISPLAY_NAME = NbBundle.getMessage(InterestingHits.class, "InterestingHits.displayName.text");
    private static final Logger logger = Logger.getLogger(InterestingHits.class.getName());
    private SleuthkitCase skCase;
    private InterestingResults interestingResults = new InterestingResults();
    
    public InterestingHits(SleuthkitCase skCase) {
        this.skCase = skCase;
        interestingResults.update();
    }
 
    private class InterestingResults extends Observable {
        private Map<String, Set<Long>> interestingItemsMap = new LinkedHashMap<>();
    
        public List<String> getSetNames() {
            List<String> setNames = new ArrayList<>(interestingItemsMap.keySet());
            Collections.sort(setNames);
            return setNames;
        }
        
        public Set<Long> getArtifactIds(String setName) {
            return interestingItemsMap.get(setName);
        }
        
        public void update() {
            interestingItemsMap.clear();
            loadArtifacts(BlackboardArtifact.ARTIFACT_TYPE.TSK_INTERESTING_FILE_HIT);
            loadArtifacts(BlackboardArtifact.ARTIFACT_TYPE.TSK_INTERESTING_ARTIFACT_HIT);
            setChanged();
            notifyObservers();
        }
    
        /*
         * Reads the artifacts of specified type, grouped by Set, and loads into the interestingItemsMap
         */
        private void loadArtifacts(BlackboardArtifact.ARTIFACT_TYPE artType) {
            ResultSet rs = null;
            try {
                int setNameId = BlackboardAttribute.ATTRIBUTE_TYPE.TSK_SET_NAME.getTypeID();
                int artId = artType.getTypeID();
                String query = "SELECT value_text,blackboard_attributes.artifact_id,attribute_type_id " //NON-NLS
                        + "FROM blackboard_attributes,blackboard_artifacts WHERE " //NON-NLS
                        + "attribute_type_id=" + setNameId //NON-NLS
                        + " AND blackboard_attributes.artifact_id=blackboard_artifacts.artifact_id" //NON-NLS
                        + " AND blackboard_artifacts.artifact_type_id=" + artId; //NON-NLS
                rs = skCase.runQuery(query);
                while (rs.next()) {
                    String value = rs.getString("value_text"); //NON-NLS
                    long artifactId = rs.getLong("artifact_id"); //NON-NLS
                    if (!interestingItemsMap.containsKey(value)) {
                        interestingItemsMap.put(value, new HashSet<Long>());
                    }
                    interestingItemsMap.get(value).add(artifactId);
                }
            } catch (SQLException ex) {
                logger.log(Level.WARNING, "SQL Exception occurred: ", ex); //NON-NLS
            }
            finally {
                if (rs != null) {
                    try {
                        skCase.closeRunQuery(rs);
                    } catch (SQLException ex) {
                       logger.log(Level.WARNING, "Error closing result set after getting artifacts", ex); //NON-NLS
                    }
                }
            }
        }
    }
    
    @Override
    public <T> T accept(AutopsyItemVisitor<T> v) {
        return v.visit(this);
    }
     
     /**
     * Node for the interesting items
     */
    public class RootNode extends DisplayableItemNode {

        public RootNode() {
            super(Children.create(new SetNameFactory(), true), Lookups.singleton(DISPLAY_NAME));
            super.setName(INTERESTING_ITEMS);
            super.setDisplayName(DISPLAY_NAME);
            this.setIconBaseWithExtension("org/sleuthkit/autopsy/images/interesting_item.png"); //NON-NLS
        }

        @Override
        public boolean isLeafTypeNode() {
            return false;
        }
                
        @Override
        public <T> T accept(DisplayableItemNodeVisitor<T> v) {
            return v.visit(this);
        }

        @Override
        protected Sheet createSheet() {
            Sheet s = super.createSheet();
            Sheet.Set ss = s.get(Sheet.PROPERTIES);
            if (ss == null) {
                ss = Sheet.createPropertiesSet();
                s.put(ss);
            }

            ss.put(new NodeProperty<>(NbBundle.getMessage(this.getClass(), "InterestingHits.createSheet.name.name"),
                                    NbBundle.getMessage(this.getClass(), "InterestingHits.createSheet.name.displayName"),
                                    NbBundle.getMessage(this.getClass(), "InterestingHits.createSheet.name.desc"),
                                    getName()));

            return s;
        }
    }
    
     private class SetNameFactory extends ChildFactory.Detachable<String> implements Observer {

         /* This should probably be in the top-level class, but the factory has nice methods
         * for its startup and shutdown, so it seemed like a cleaner place to register the
         * property change listener.
         */
        private final PropertyChangeListener pcl = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                String eventType = evt.getPropertyName();
                
                if (eventType.equals(IngestManager.IngestEvent.DATA.toString())) {
                    if ((((ModuleDataEvent) evt.getOldValue()).getArtifactType() == BlackboardArtifact.ARTIFACT_TYPE.TSK_INTERESTING_ARTIFACT_HIT) ||
                            (((ModuleDataEvent) evt.getOldValue()).getArtifactType() == BlackboardArtifact.ARTIFACT_TYPE.TSK_INTERESTING_FILE_HIT)) {
                        interestingResults.update();
                    }
                }
                else if (eventType.equals(IngestManager.IngestEvent.INGEST_JOB_COMPLETED.toString())
                || eventType.equals(IngestManager.IngestEvent.INGEST_JOB_CANCELLED.toString())) {
                    interestingResults.update();
                }
            }
        };

        @Override
        protected void addNotify() {
            IngestManager.addPropertyChangeListener(pcl);
            interestingResults.update();
            interestingResults.addObserver(this);
        }

        @Override
        protected void removeNotify() {
            IngestManager.removePropertyChangeListener(pcl);
            interestingResults.deleteObserver(this);
        }
        
        @Override
        protected boolean createKeys(List<String> list) {
            list.addAll(interestingResults.getSetNames());
            return true;
        }

        @Override
        protected Node createNodeForKey(String key) {
            return new SetNameNode(key);
        }

        @Override
        public void update(Observable o, Object arg) {
            refresh(true);
        }
    }
     
    public class SetNameNode extends DisplayableItemNode implements Observer {
        private String setName;
        public SetNameNode(String setName) {//, Set<Long> children) {
            super(Children.create(new HitFactory(setName), true), Lookups.singleton(setName));
            this.setName = setName;
            super.setName(setName);
            updateDisplayName();
            this.setIconBaseWithExtension("org/sleuthkit/autopsy/images/interesting_item.png"); //NON-NLS
            interestingResults.addObserver(this);
        }
        
        private void updateDisplayName() {
            super.setDisplayName(setName + " (" + interestingResults.getArtifactIds(setName).size() + ")");
        }

        @Override
        public boolean isLeafTypeNode() {
            return true;
        }

        @Override
        protected Sheet createSheet() {
            Sheet s = super.createSheet();
            Sheet.Set ss = s.get(Sheet.PROPERTIES);
            if (ss == null) {
                ss = Sheet.createPropertiesSet();
                s.put(ss);
            }

            ss.put(new NodeProperty<>(NbBundle.getMessage(this.getClass(), "InterestingHits.createSheet.name.name"),
                                    NbBundle.getMessage(this.getClass(), "InterestingHits.createSheet.name.name"),
                                    NbBundle.getMessage(this.getClass(), "InterestingHits.createSheet.name.desc"),
                                    getName()));

            return s;
        }

        @Override
        public <T> T accept(DisplayableItemNodeVisitor<T> v) {
            return v.visit(this);
        }

        @Override
        public void update(Observable o, Object arg) {
            updateDisplayName();
        }
    }
     
    private class HitFactory extends ChildFactory<Long> implements Observer {
        private String setName;

        private HitFactory(String setName) {
            super();
            this.setName = setName;
            interestingResults.addObserver(this);
        }

        @Override
        protected boolean createKeys(List<Long> list) {
            for (long l : interestingResults.getArtifactIds(setName)) {
                list.add(l);
            }
            return true;
        }

        @Override
        protected Node createNodeForKey(Long l) {
            try {
                return new BlackboardArtifactNode(skCase.getBlackboardArtifact(l));
            } catch (TskCoreException ex) {
                Exceptions.printStackTrace(ex);
                return null;
            }
        }

        @Override
        public void update(Observable o, Object arg) {
            refresh(true);
        }
    }
}
