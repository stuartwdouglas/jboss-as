package org.wildfly.build.plugin;

import org.jboss.modules.ModuleIdentifier;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Stuart Douglas
 */
public class ModuleParseResult {
    final File moduleXmlFile;
    final List<ModuleDependency> dependencies = new ArrayList<ModuleDependency>();
    ModuleIdentifier identifier;

    public ModuleParseResult(File moduleXmlFile) {
        this.moduleXmlFile = moduleXmlFile;
    }

    public File getModuleXmlFile() {
        return moduleXmlFile;
    }

    public List<ModuleDependency> getDependencies() {
        return dependencies;
    }

    public ModuleIdentifier getIdentifier() {
        return identifier;
    }

    static class ModuleDependency {
        private final ModuleIdentifier moduleId;
        private final boolean optional;

        ModuleDependency(ModuleIdentifier moduleId, boolean optional) {
            this.moduleId = moduleId;
            this.optional = optional;
        }

        ModuleIdentifier getModuleId() {
            return moduleId;
        }

        boolean isOptional() {
            return optional;
        }

        @Override
        public String toString() {
            return "[" + moduleId + (optional ? ",optional=true" : "") + "]";
        }
    }
}
