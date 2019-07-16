import com.intellij.database.model.DasTable
import com.intellij.database.model.ObjectKind
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil

/*
* Available context bindings:
*   SELECTION   Iterable<DasObject>
*   PROJECT     project
*   FILES       files helper
*/

/* 包名，也可以直接修改生成的 Java 文件 */
packageName = "com.sample;"
typeMapping = [
        (~/(?i)int/)                      : "Integer",
        (~/(?i)float|double|decimal|real/): "double",
        (~/(?i)datetime|timestamp/)       : "LocalDateTime",
        (~/(?i)date/)                     : "LocalDate",
        (~/(?i)time/)                     : "LocalDateTime",
        (~/(?i)/)                         : "String"
]

FILES.chooseDirectoryAndSave("Choose directory", "Choose where to store generated files") { dir ->
    SELECTION.filter { it instanceof DasTable && it.getKind() == ObjectKind.TABLE }.each { generate(it, dir) }
}

def generate(table, dir) {
    def className = javaName(table.getName(), true)
    def fields = calcFields(table)
    new File(dir, className + ".java").withPrintWriter('UTF-8') { out -> generate(out, table, className, fields) }
}

def generate(out, table, className, fields) {
    out.println "package $packageName"
    /* 可在此添加需要导入的包名，也可通过 IDE 批量修改生成的 Java 文件 */
    out.println ""
    out.println "import com.baomidou.mybatisplus.annotation.*;"
    out.println "import io.swagger.annotations.ApiModel;"
    out.println "import io.swagger.annotations.ApiModelProperty;"
    out.println ""
    out.println "/** * @author jason */"
    out.println "@ApiModel(value = \"${table.getComment()}\")"
    out.println "@TableName(value = \"${table.getName()}\")"
    out.println "public class $className {"
    out.println ""
    fields.each() {
        out.println "/** * ${it.comment} */"
        if (it.annos != "") out.println "  ${it.annos}"
        out.println "  private ${it.type} ${it.name};"
        out.println ""
    }

    out.println ""
    fields.each() {
        out.println "public static final String COL_${it.uppercol} = \"${it.name}\" ;"
        out.println ""
    }

    out.println ""
    fields.each() {
        out.println ""
        out.println "  public ${it.type} get${it.name.capitalize()}() {"
        out.println "    return ${it.name};"
        out.println "  }"
        out.println ""
        out.println "  public void set${it.name.capitalize()}(${it.type} ${it.name}) {"
        out.println "    this.${it.name} = ${it.name};"
        out.println "  }"
        out.println ""
    }
    out.println "}"
}

def calcFields(table) {
    DasUtil.getColumns(table).reduce([]) { fields, col ->
        def spec = Case.LOWER.apply(col.getDataType().getSpecification())
        def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }.value
        fields += [[
                           name    : javaName(col.getName(), false),
                           type    : typeStr,
                           col     : col.getName(),
                           comment : col.getComment(),
                           annos   : customAnnotation(col),
                           uppercol: Case.UPPER.apply(col.getName())
                   ]]
    }
}

static def customAnnotation(col) {
    if ("id" == col.getName()) {
        return "@TableId(value = \"" + col.getName() + "\" , type = IdType.ID_WORKER_STR) \n @ApiModelProperty(value = \"" + col.getComment() + "\")"
    }
    if ("created_by" == col.getName()) {
        return "@TableField(value = \"" + col.getName() + "\", fill = FieldFill.INSERT) \n @ApiModelProperty(value = \"" + col.getComment() + "\")"
    }
    if ("created_date" == col.getName()) {
        return "@TableField(value = \"" + col.getName() + "\", fill = FieldFill.INSERT) \n @ApiModelProperty(value = \"" + col.getComment() + "\")"
    }
    if ("last_modified_by" == col.getName()) {
        return "@TableField(value = \"" + col.getName() + "\", fill = FieldFill.INSERT_UPDATE) \n @ApiModelProperty(value = \"" + col.getComment() + "\")"
    }
    if ("last_modified_date" == col.getName()) {
        return "@TableField(value = \"" + col.getName() + "\", fill = FieldFill.INSERT_UPDATE)  \n @ApiModelProperty(value = \"" + col.getComment() + "\")"
    }
    if ("enabled" == col.getName()) {
        return "@TableField(value = \"" + col.getName() + "\") \n @TableLogic(value = \"1\", delval = \"0\") \n @ApiModelProperty(value = \"" + col.getComment() + "\")"
    }
    if ("version" == col.getName()) {
        return "@TableField(value = \"" + col.getName() + "\") \n  @Version \n @ApiModelProperty(value = \"" + col.getComment() + "\")"
    }
    if ("order_no" == col.getName()) {
        return "@TableField(value = \"" + col.getName() + "\", fill = FieldFill.INSERT) \n  @Version \n @ApiModelProperty(value = \"" + col.getComment() + "\")"
    }
    return "@TableField(value = \"" + col.getName() + "\") \n @ApiModelProperty(value = \"" + col.getComment() + "\")"
}

def javaName(str, capitalize) {
    def s = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
            .collect { Case.LOWER.apply(it).capitalize() }
            .join("")
            .replaceAll(/[^\p{javaJavaIdentifierPart}[_]]/, "_")
    capitalize || s.length() == 1 ? s : Case.LOWER.apply(s[0]) + s[1..-1]
}