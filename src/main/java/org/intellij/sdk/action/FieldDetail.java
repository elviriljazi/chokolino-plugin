package org.intellij.sdk.action;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiModifierList;

 class FieldDetail {
    private final String type;
    private String snakeCaseName = "";
    private String camelCaseName = "";

    public FieldDetail(PsiField field) {
        this.type = field.getType().getPresentableText();
        PsiModifierList modifierList = field.getModifierList();
        if (modifierList == null) {
            return;
        }
        PsiAnnotation annotation = modifierList
                .findAnnotation("com.google.gson.annotations.SerializedName");
        if (annotation == null) {
            return;
        }
        PsiAnnotationMemberValue annotationMemberValue = annotation.findAttributeValue("value");
        if (annotationMemberValue == null) {
            return;
        }
        this.snakeCaseName = annotationMemberValue.getText();
        this.camelCaseName = field.getName();
    }

    public String getType() {
        return type;
    }

    public String getSnakeCaseName() {
        return snakeCaseName;
    }

    public String getCamelCaseName() {
        return camelCaseName;
    }
}