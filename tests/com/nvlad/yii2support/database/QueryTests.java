package com.nvlad.yii2support.database;

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.database.psi.DbDataSource;
import com.intellij.database.psi.DbPsiFacade;
import com.jetbrains.php.lang.PhpFileType;
import com.nvlad.yii2support.PluginTestCase;
import com.nvlad.yii2support.database.fixtures.TestDataSource;

import java.io.File;
import java.util.List;

/**
 * Created by oleg on 03.04.2017.
 */
public class QueryTests extends PluginTestCase {



    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myFixture.configureFromExistingVirtualFile(myFixture.copyFileToProject("classes.php"));


        DbPsiFacade facade =  DbPsiFacade.getInstance(myFixture.getProject());
        if (facade.getDataSources().size() == 0) {
            TestDataSource source = new TestDataSource(myFixture.getProject());
            facade.getDataSources().add(source);
            List<DbDataSource> dataSources = facade.getDataSources();
        }
    }

    @Override
    protected String getTestDataPath() {
        return new File(this.getClass().getResource("fixtures").getFile()).getAbsolutePath();
    }

    public void testQuery_Empty() {

        myFixture.configureByText(PhpFileType.INSTANCE,   "<?php \n" +
                " (new \\yii\\db\\Query())->where('<caret>')\n" +
                ";");
        myFixture.completeBasic();
        assertEquals(2, myFixture.getLookupElementStrings().toArray().length);
    }

    public void testQuery_Quoting() {

        myFixture.configureByText(PhpFileType.INSTANCE,   "<?php \n" +
                " (new \\yii\\db\\Query())->where('{{<caret>')\n" +
                ";");
        myFixture.completeBasic();
        assertEquals(2, myFixture.getLookupElementStrings().toArray().length);
    }

    public void testQuery_QuotingDbPrefix() {

        myFixture.configureByText(PhpFileType.INSTANCE,   "<?php \n" +
                " (new \\yii\\db\\Query())->where('{{%<caret>')\n" +
                ";");
        myFixture.completeBasic();
        assertEquals(2, myFixture.getLookupElementStrings().size());
    }

    public void testQuery_Prefix() {

        myFixture.configureByText(PhpFileType.INSTANCE,   "<?php \n" +
                " (new \\yii\\db\\Query())->where('per<caret>');");
        myFixture.complete(CompletionType.BASIC);
        assertEquals("'person'", myFixture.getFile().findElementAt(myFixture.getCaretOffset()).getParent().getText());
    }

    public void testQuery_SuffixQuotingDbPrefix() {

        myFixture.configureByText(PhpFileType.INSTANCE,   "<?php \n" +
                " (new \\yii\\db\\Query())->where('{{%per<caret>');");
        myFixture.completeBasic();
        assertEquals("'{{%person'", myFixture.getFile().findElementAt(myFixture.getCaretOffset()).getParent().getText());
    }

    public void testQuery_Columns() {

        myFixture.configureByText(PhpFileType.INSTANCE,   "<?php \n" +
                " (new \\yii\\db\\Query())->where('person.<caret>\n" +
                ";");
        myFixture.completeBasic();
        assertEquals(3, myFixture.getLookupElementStrings().toArray().length);
    }

    public void testQuery_ColumnPrefix() {

        myFixture.configureByText(PhpFileType.INSTANCE,   "<?php \n" +
                " (new \\yii\\db\\Query())->where('person.na<caret>\n" +
                ";");
        myFixture.completeBasic();

        assertEquals("name", myFixture.getLookupElementStrings().get(0));
        assertEquals("surname", myFixture.getLookupElementStrings().get(1));
    }

    public void testQuery_ColumnPrefixTableQuoting() {

        myFixture.configureByText(PhpFileType.INSTANCE,   "<?php \n" +
                " (new \\yii\\db\\Query())->where('{{person}}.na<caret>');");
        myFixture.completeBasic();
        assertEquals("'{{person}}.name'", myFixture.getFile().findElementAt(myFixture.getCaretOffset()).getParent().getText());
    }

    public void testQuery_ColumnQuotingAndPrefixTableQuoting() {

        myFixture.configureByText(PhpFileType.INSTANCE,   "<?php \n" +
                " (new \\yii\\db\\Query())->where('{{person}}.[[na<caret>');");
        myFixture.completeBasic();
        assertEquals("'{{person}}.[[name'", myFixture.getFile().findElementAt(myFixture.getCaretOffset()).getParent().getText());
    }

}
