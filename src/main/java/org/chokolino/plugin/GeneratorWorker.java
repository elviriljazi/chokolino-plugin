package org.chokolino.plugin;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.webSymbols.utils.NameCaseUtils;
import org.jetbrains.java.generate.psi.PsiAdapter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GeneratorWorker {

    public static GeneratorWorker getInstance() {
        return new GeneratorWorker();
    }

    public void generate(AnActionEvent event, boolean buildConstructor, boolean buildPopulate) {
        Project project = event.getProject();
        assert project != null;
        PsiJavaFile file = (PsiJavaFile) event.getDataContext().getData(CommonDataKeys.PSI_FILE);
        assert file != null;
        Editor editor = event.getDataContext().getData(CommonDataKeys.EDITOR);
        assert editor != null;

        PsiClass psiClass = file.getClasses()[0];

        List<FieldDetail> fieldsData = getFieldsData(psiClass.getFields());
        CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(project);
        WriteCommandAction.runWriteCommandAction(project, () ->
        {
            try {
                if (buildConstructor) {
                    PsiMethod constructor = createConstructor(fieldsData, project);
                    psiClass.add(constructor);

                    codeStyleManager.reformat(psiClass);
                }

                if (buildPopulate) {
                    PsiMethod populatePs = createPopulatePs(fieldsData, project);
                    psiClass.add(populatePs);

                    codeStyleManager.reformat(psiClass);
                }

                PsiAdapter.addImportStatement(file, "java.sql.*");
                PsiAdapter.addImportStatement(file, "java.util.*");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


    private PsiMethod createConstructor(List<FieldDetail> fields, Project project) {
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
        PsiMethod constructor = elementFactory.createConstructor();

        PsiType resultSetType = elementFactory.createTypeByFQClassName("ResultSet");
        PsiParameter resultSetParam = elementFactory.createParameter("rs", resultSetType, constructor);

        PsiType aliasType = elementFactory.createTypeByFQClassName("String");
        PsiParameter aliasParam = elementFactory.createParameter("alias", aliasType, constructor);

        PsiJavaCodeReferenceElement exceptionReference =
                elementFactory.createReferenceElementByFQClassName("Exception", constructor.getResolveScope());

        constructor.getThrowsList().add(exceptionReference);
        constructor.getParameterList().add(resultSetParam);
        constructor.getParameterList().add(aliasParam);

        for (FieldDetail fieldDetail : fields) {
            String statement = ("set$camelCase(rs.get$type(alias + $snakeCase));")
                    .replace("$type", convertType(fieldDetail.getType()))
                    .replace("$snakeCase", fieldDetail.getSnakeCaseName())
                    .replace("$camelCase", NameCaseUtils.toPascalCase(fieldDetail.getCamelCaseName()));

            PsiStatement row = elementFactory.createStatementFromText(statement, constructor);
            PsiCodeBlock body = constructor.getBody();
            if (body != null) {
                body.add(row);
            }
        }
        return constructor;
    }

    private PsiMethod createPopulatePs(List<FieldDetail> fields, Project project) {
        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
        PsiMethod populatePs = elementFactory.createMethod("populatePs", PsiType.VOID);

        PsiType resultSetType = elementFactory.createTypeByFQClassName("NamedParameterStatement");
        PsiParameter resultSetParam = elementFactory.createParameter("ps", resultSetType, populatePs);

        PsiJavaCodeReferenceElement exceptionReference =
                elementFactory.createReferenceElementByFQClassName("Exception", populatePs.getResolveScope());

        populatePs.getThrowsList().add(exceptionReference);
        populatePs.getParameterList().add(resultSetParam);

        for (FieldDetail fieldDetail : fields) {
            String statement = "ps.set$type($snakeCase,get$camelCase());"
                    .replace("$type", fieldDetail.getType())
                    .replace("$snakeCase", fieldDetail.getSnakeCaseName())
                    .replace("$camelCase", NameCaseUtils.toPascalCase(fieldDetail.getCamelCaseName()));

            PsiStatement row = elementFactory.createStatementFromText(statement, populatePs);
            PsiCodeBlock body = populatePs.getBody();
            if (body != null) {
                body.add(row);
            }
        }
        return populatePs;
    }

    private List<FieldDetail> getFieldsData(PsiField[] fields) {
        return Arrays.stream(fields).map(FieldDetail::new).collect(Collectors.toList());
    }

    private String convertType(String type) {
        if ("Integer".equals(type)) {
            return "Int";
        }
        return type;
    }

}
