package com.nvlad.yii2support.common;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.nvlad.yii2support.utils.Yii2SupportSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class YiiApplicationUtils {
    private static Map<Project, VirtualFile> yiiRootPaths = new HashMap<>();

    @Nullable
    public static String getYiiRootPath(Project project) {
        VirtualFile yiiRoot = getYiiRootVirtualFile(project);

        return yiiRoot == null ? null : yiiRoot.getPath();
    }

    @Nullable
    public static String getYiiRootUrl(Project project) {
        VirtualFile yiiRoot = getYiiRootVirtualFile(project);

        return yiiRoot == null ? null : yiiRoot.getUrl();
    }

    public static void resetYiiRootPath(Project project) {
        yiiRootPaths.remove(project);
    }

    @NotNull
    public static String getApplicationName(@NotNull PsiFile file) {
        return getApplicationName(FileUtil.getVirtualFile(file), file.getProject());
    }

    @NotNull
    public static String getApplicationName(@NotNull VirtualFile file, @NotNull Project project) {
        VirtualFile yiiRoot = getYiiRootVirtualFile(project);
        if (yiiRoot == null) {
            return "";
        }

        if (yiiRoot.findChild("controllers") == null) {
            final String fileUrl = file.getUrl();
            if (!fileUrl.startsWith(yiiRoot.getUrl())) {
                return "";
            }

            int yiiRootLength = yiiRoot.getUrl().length();
            int slashIndex = fileUrl.indexOf("/", yiiRootLength + 1);
            if (slashIndex == -1) {
                return "";
            }

            return fileUrl.substring(yiiRootLength + 1, slashIndex);
        }
        return "app";
    }

    @Nullable
    public static VirtualFile getYiiRootVirtualFile(Project project) {
        if (yiiRootPaths.containsKey(project)) {
            return yiiRootPaths.get(project);
        }

        String path = Yii2SupportSettings.getInstance(project).yiiRootPath;
        VirtualFile yiiRootPath;
        if (path == null) {
            yiiRootPath = project.getBaseDir();
        } else {
            LocalFileSystem fileSystem = LocalFileSystem.getInstance();
            yiiRootPath = fileSystem.refreshAndFindFileByPath(path);
            if (yiiRootPath == null) {
                yiiRootPath = project.getBaseDir();
                path = path.replace('\\', '/');
                if (path.startsWith("./")) {
                    path = path.substring(2);
                }
                if (path.startsWith("/")) {
                    path = path.substring(1);
                }
                List<String> pathEntries = StringUtil.split(path, "/");
                for (String pathEntry : pathEntries) {
                    yiiRootPath = yiiRootPath.findChild(pathEntry);
                    if (yiiRootPath == null) {
                        break;
                    }
                }
            }
        }

        yiiRootPaths.put(project, yiiRootPath);
        return yiiRootPath;
    }
}
