package org.workcraft;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.workcraft.plugins.shared.CommonDebugSettings;
import org.workcraft.util.LogUtils;

public class CompatibilityManager {
    private static final Pattern versionPattern = Pattern.compile("<version major=\"([0-9]+)\" minor=\"([0-9]+)\" revision=\"([0-9]+)\" status=\"(.+?)\"/>");
    private static final Pattern modelNamePattern = Pattern.compile("<model class=\"(.+?)\" ref=\"\">");
    private static final Pattern classNamePattern = Pattern.compile("<([A-Z]\\S*).*>");

    @SuppressWarnings("serial")
    private class Replacement extends HashMap<String, String> { }

    @SuppressWarnings("serial")
    private class ContextualReplacement extends HashMap<String, Replacement> { }

    @SuppressWarnings("serial")
    private class NestedContextualReplacement extends HashMap<String, ContextualReplacement> { }

    private class ReplacementData {
        private final Replacement meta = new Replacement();
        private final Replacement model = new Replacement();
        private final ContextualReplacement global = new ContextualReplacement();
        private final NestedContextualReplacement local = new NestedContextualReplacement();
    }

    @SuppressWarnings("serial")
    private class VersionedReplacementData extends HashMap<Version, ReplacementData> { }

    private final VersionedReplacementData versionedReplacementData = new VersionedReplacementData();

    private ReplacementData getReplacementData(Version version) {
        ReplacementData result = versionedReplacementData.get(version);
        if (result == null) {
            result = new ReplacementData();
            versionedReplacementData.put(version, result);
        }
        return result;
    }

    private HashSet<ReplacementData> getApplicableData(Version version) {
        HashSet<ReplacementData> result = new HashSet<>();
        for (Version sinceVersion: versionedReplacementData.keySet()) {
            if ((version == null) || (version.compareTo(sinceVersion) < 0)) {
                ReplacementData data = getReplacementData(sinceVersion);
                result.add(data);
            }
        }
        return result;
    }

    public void registerMetaReplacement(Version version, String oldMetaData, String metaData) {
        ReplacementData replacementData = getReplacementData(version);
        replacementData.meta.put(oldMetaData, metaData);
    }

    public void registerModelReplacement(Version version, String oldModelName, String modelName) {
        ReplacementData replacementData = getReplacementData(version);
        replacementData.model.put(oldModelName, modelName);
    }

    public void registerGlobalReplacement(Version version, String modelName, String pattern, String replacement) {
        ReplacementData replacementData = getReplacementData(version);
        Replacement replacementMap = replacementData.global.get(modelName);
        if (replacementMap == null) {
            replacementMap = new Replacement();
            replacementData.global.put(modelName, replacementMap);
        }
        replacementMap.put(pattern, replacement);
    }

    public void registerContextualReplacement(Version version, String modelName, String className, String pattern, String replacement) {
        ReplacementData replacementData = getReplacementData(version);
        ContextualReplacement contextualMap = replacementData.local.get(modelName);
        if (contextualMap == null) {
            contextualMap = new ContextualReplacement();
            replacementData.local.put(modelName, contextualMap);
        }
        Replacement replacementMap = contextualMap.get(className);
        if (replacementMap == null) {
            replacementMap = new Replacement();
            contextualMap.put(className, replacementMap);
        }
        replacementMap.put(pattern, replacement);
    }

    private String replace(String line, Map.Entry<String, String> replacement, String message) {
        String newline = line.replaceAll(replacement.getKey(), replacement.getValue());
        if (CommonDebugSettings.getVerboseCompatibilityManager()
                && (message != null) && !line.equals(newline)) {
            LogUtils.logInfoLine("Compatibility management: " + message);
            LogUtils.logInfoLine("  old: " + replacement.getKey());
            LogUtils.logInfoLine("  new: " + replacement.getValue());
        }
        return newline;
    }

    private String replaceMetaData(Version version, String line) {
        for (ReplacementData data: getApplicableData(version)) {
            for (Map.Entry<String, String> replacement: data.meta.entrySet()) {
                if (line.contains(replacement.getKey())) {
                    line = replace(line, replacement, "legacy meta data");
                }
            }
        }
        return line;
    }

    private String replaceModelName(Version version, String line) {
        for (ReplacementData data: getApplicableData(version)) {
            for (Map.Entry<String, String> replacement: data.model.entrySet()) {
                if (line.contains(replacement.getKey())) {
                    line = replace(line, replacement, "legacy model class");
                }
            }
        }
        return line;
    }

    private String replaceGlobalEntry(Version version, String modelName, String line) {
        for (ReplacementData data: getApplicableData(version)) {
            Replacement replacementMap = data.global.get(modelName);
            if (replacementMap != null) {
                for (Map.Entry<String, String> replacement: replacementMap.entrySet()) {
                    line = replace(line, replacement, "global replacement");
                }
            }
        }
        return line;
    }

    private String replaceContextualEntry(Version version, String modelName, String className, String line) {
        for (ReplacementData data: getApplicableData(version)) {
            HashMap<String, Replacement> contextualMap = data.local.get(modelName);
            if (contextualMap != null) {
                Replacement replacementMap = contextualMap.get(className);
                if (replacementMap != null) {
                    for (Map.Entry<String, String> replacement: replacementMap.entrySet()) {
                        line = replace(line, replacement, "contextual replacement for " + className);
                    }
                }
            }
        }
        return line;
    }

    private String replaceEntry(Version version, String modelName, String className, String line) {
        line = replaceGlobalEntry(version, modelName, line);
        line = replaceContextualEntry(version, modelName, className, line);
        return line;
    }

    private Version extractVersion(String line) {
        Version result = null;
        Matcher matcher = versionPattern.matcher(line);
        if (matcher.find()) {
            int major = Integer.parseInt(matcher.group(1));
            int minor = Integer.parseInt(matcher.group(2));
            int revision = Integer.parseInt(matcher.group(3));
            Version.Status status = Version.Status.fromString(matcher.group(4));
            result = new Version(major, minor, revision, status);
        }
        return result;
    }

    private String extractModelName(String line) {
        String result = null;
        Matcher matcher = modelNamePattern.matcher(line);
        if (matcher.find()) {
            result = matcher.group(1);
        }
        return result;
    }

    private String extractClassName(String line) {
        String result = null;
        Matcher matcher = classNamePattern.matcher(line);
        if (matcher.find()) {
            result = matcher.group(1);
        }
        return result;
    }

    public ByteArrayInputStream process(InputStream is) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ZipInputStream zis = new ZipInputStream(is);
        ZipOutputStream zos = new ZipOutputStream(out);
        ZipEntry zei;
        BufferedReader br = new BufferedReader(new InputStreamReader(zis));
        try {
            while ((zei = zis.getNextEntry()) != null) {
                ZipEntry zeo = new ZipEntry(zei.getName());
                boolean isMetaEntry = "meta".equals(zei.getName());
                zos.putNextEntry(zeo);
                Version version = null;
                String modelName = null;
                String className = null;
                String line = null;
                while ((line = br.readLine()) != null) {
                    if (isMetaEntry) {
                        if (version == null) {
                            version = extractVersion(line);
                        }
                        byte[] data = replaceMetaData(version, line).getBytes();
                        zos.write(data, 0, data.length);
                    } else if (modelName == null) {
                        String processedLine = replaceModelName(version, line);
                        byte[] data = processedLine.getBytes();
                        zos.write(data, 0, data.length);
                        modelName = extractModelName(processedLine);
                    } else {
                        String s = extractClassName(line);
                        if (s != null) {
                            className = s;
                        }
                        byte[] data = replaceEntry(version, modelName, className, line).getBytes();
                        zos.write(data, 0, data.length);
                    }
                }
                zis.closeEntry();
                zos.closeEntry();
            }
            zos.close();
            out.close();
        } catch (IOException e) {
        }
        return new ByteArrayInputStream(out.toByteArray());
    }

}
