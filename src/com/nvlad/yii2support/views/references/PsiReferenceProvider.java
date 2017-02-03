package com.nvlad.yii2support.views.references;

import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by NVlad on 02.01.2017.
 */
class PsiReferenceProvider extends com.intellij.psi.PsiReferenceProvider {
    @NotNull
    @Override
    public PsiReference[] getReferencesByElement(@NotNull PsiElement psiElement, @NotNull ProcessingContext processingContext) {
        List<PsiReference> references = new ArrayList<>();

        PsiReference reference = new PsiReference(psiElement);
        references.add(reference);

        return references.toArray(new PsiReference[references.size()]);
    }
}
