package com.nvlad.yii2support.views.inspections;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ArrayUtil;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.lang.inspections.PhpInspection;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.FunctionReference;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.php.lang.psi.visitors.PhpElementVisitor;
import com.nvlad.yii2support.common.PhpUtil;
import com.nvlad.yii2support.common.YiiApplicationUtils;
import com.nvlad.yii2support.utils.Yii2SupportSettings;
import com.nvlad.yii2support.views.entities.ViewInfo;
import com.nvlad.yii2support.views.entities.ViewParameter;
import com.nvlad.yii2support.views.entities.ViewResolve;
import com.nvlad.yii2support.views.index.ViewFileIndex;
import com.nvlad.yii2support.views.util.ViewUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Created by NVlad on 23.01.2017.
 */
public class RequireParameterInspection extends PhpInspection {
    @NotNull
    @Override
    public String getShortName() {
        return "RequireParameterInspection";
    }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder problemsHolder, boolean isOnTheFly) {
        return new PhpElementVisitor() {
            @Override
            public void visitPhpMethodReference(MethodReference reference) {
                if (!ViewUtil.isValidRenderMethod(reference)) {
                    return;
                }

                final String name = reference.getName();
                if (name == null || !ArrayUtil.contains(name, ViewUtil.renderMethods)) {
                    return;
                }

                final PsiElement[] renderParameters = reference.getParameters();
                if (renderParameters.length == 0 || !(renderParameters[0] instanceof StringLiteralExpression)) {
                    return;
                }

                final ViewResolve resolve = ViewUtil.resolveView(renderParameters[0]);
                if (resolve == null) {
                    return;
                }

                String key = resolve.key;
                if (FileUtilRt.getExtension(key).isEmpty()) {
                    key = key + '.' + Yii2SupportSettings.getInstance(reference.getProject()).defaultViewExtension;
                }

                final Project project = reference.getProject();
                final Collection<ViewInfo> views = FileBasedIndex.getInstance()
                        .getValues(ViewFileIndex.identity, key, GlobalSearchScope.projectScope(project));
                final String application = YiiApplicationUtils.getApplicationName(reference.getContainingFile());
                views.removeIf(viewInfo -> !application.equals(viewInfo.application));
                if (views.size() == 0) {
                    return;
                }

                final Collection<ViewParameter> viewParameters = new HashSet<>();
                for (ViewInfo view : views) {
                    viewParameters.addAll(view.parameters);
                }
                if (viewParameters.size() == 0) {
                    return;
                }

                final Collection<String> existKeys;
                if (renderParameters.length > 1) {
                    if (renderParameters[1] instanceof ArrayCreationExpression) {
                        existKeys = PhpUtil.getArrayKeys((ArrayCreationExpression) renderParameters[1]);
                    } else if (renderParameters[1] instanceof FunctionReference) {
                        FunctionReference function = (FunctionReference) renderParameters[1];
                        if (function.getName() != null && function.getName().equals("compact")) {
                            existKeys = new HashSet<>();
                            for (PsiElement element : function.getParameters()) {
                                if (element instanceof StringLiteralExpression) {
                                    existKeys.add(((StringLiteralExpression) element).getContents());
                                }
                            }
                        } else {
                            return;
                        }
                    } else {
                        return;
                    }
                } else {
                    existKeys = new HashSet<>();
                }

                viewParameters.removeIf(viewParameter -> existKeys.contains(viewParameter.name));
                if (viewParameters.size() == 0) {
                    return;
                }

                String description = "View " + renderParameters[0].getText() + " require ";
                final Iterator<ViewParameter> parameterIterator = viewParameters.iterator();
                if (!isOnTheFly) {
                    while (parameterIterator.hasNext()) {
                        final ViewParameter parameter = parameterIterator.next();
                        final String problemDescription = description + "\"" + parameter.name + "\" parameter.";
                        problemsHolder.registerProblem(reference, problemDescription, new RequireParameterLocalQuickFix(parameter));
                    }
                    return;
                }

                final Collection<LocalQuickFix> fixes = new HashSet<>();
                if (viewParameters.size() > 1) {
                    fixes.add(new RequireParameterLocalQuickFix(viewParameters.toArray(new ViewParameter[0])));
                    StringBuilder parameterString = new StringBuilder();
                    ViewParameter parameter = parameterIterator.next();
                    while (parameterIterator.hasNext()) {
                        if (parameterString.length() > 0) {
                            parameterString.append(", ");
                        }
                        parameterString.append("\"").append(parameter.name).append("\"");
                        fixes.add(new RequireParameterLocalQuickFix(parameter));
                        parameter = parameterIterator.next();
                    }
                    parameterString.append(" and \"").append(parameter.name).append("\" parameters.");
                    description += parameterString.toString();
                    fixes.add(new RequireParameterLocalQuickFix(parameter));
                } else {
                    ViewParameter parameter = parameterIterator.next();
                    description += "\"" + parameter.name + "\" parameter.";
                    fixes.add(new RequireParameterLocalQuickFix(parameter));
                }
                problemsHolder.registerProblem(reference, description, fixes.toArray(new LocalQuickFix[0]));
            }
        };
    }
}
