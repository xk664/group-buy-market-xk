package cn.bugstack.ai.rag.parser;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MultiFormatDocumentParserTest {

    private final MultiFormatDocumentParser parser = new MultiFormatDocumentParser();

    @Test
    public void testParseTxt() throws Exception {
        Path tmp = Files.createTempFile("note", ".txt");
        Files.write(tmp, "退款政策：1-3 个工作日到账。".getBytes("UTF-8"));
        ParsedDocument doc = parser.parse(tmp);
        assertTrue("标题应来自文件名: " + doc.getTitle(), doc.getTitle().startsWith("note"));
        assertTrue(doc.getRawText().contains("退款政策"));
    }

    @Test
    public void testParseXlsxToMarkdownTable() throws Exception {
        Path tmp = Files.createTempFile("params", ".xlsx");
        try (Workbook wb = new XSSFWorkbook(); FileOutputStream out = new FileOutputStream(tmp.toFile())) {
            Sheet sheet = wb.createSheet("参数");
            String[][] rows = {
                    {"goodsId", "商品名称", "原价"},
                    {"9890001", "蓝牙耳机", "399"},
                    {"9890002", "保温杯", "129"}
            };
            for (int r = 0; r < rows.length; r++) {
                Row row = sheet.createRow(r);
                for (int c = 0; c < rows[r].length; c++) {
                    row.createCell(c).setCellValue(rows[r][c]);
                }
            }
            wb.write(out);
        }
        ParsedDocument doc = parser.parse(tmp);
        assertTrue("应转成 Markdown 表格表头: " + doc.getRawText(), doc.getRawText().contains("| goodsId"));
        assertTrue("应包含数据行: " + doc.getRawText(), doc.getRawText().contains("9890001"));
        assertTrue("应包含分隔行: " + doc.getRawText(), doc.getRawText().contains("---"));
    }

}