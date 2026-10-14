package com.course.sims.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * 多工作表 Excel 导出工具。
 *
 * <p>统计报表需要把"概览、班级排名、课程统计、成绩分布、专业分布"放在同一个
 * 工作簿的不同工作表中，本类在 {@link ExcelUtil} 的单表导出之外补齐这一能力。</p>
 *
 * @author 课程设计第 2 小组 - 成员 E
 * @version 1.0.0
 */
public final class MultiSheetExcel {

    private MultiSheetExcel() {
    }

    /**
     * 导出多工作表 Excel。
     *
     * @param file       目标文件
     * @param reportName 报表名称（写入第一个工作表标题上方注释行，此处用于文件名语义）
     * @param sheetNames 各工作表名称
     * @param headers    各工作表表头
     * @param rowsList   各工作表数据行
     * @throws IOException 写入失败
     */
    public static void export(File file, String reportName, String[] sheetNames, String[][] headers,
                              List<Object[]>[] rowsList) throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            CellStyle headStyle = headStyle(wb);
            CellStyle bodyStyle = bodyStyle(wb);
            int count = Math.min(sheetNames.length, Math.min(headers.length, rowsList.length));
            for (int i = 0; i < count; i++) {
                Sheet sheet = wb.createSheet(sheetNames[i]);
                Row headRow = sheet.createRow(0);
                for (int c = 0; c < headers[i].length; c++) {
                    Cell cell = headRow.createCell(c);
                    cell.setCellValue(headers[i][c]);
                    cell.setCellStyle(headStyle);
                }
                int rowIndex = 1;
                for (Object[] data : rowsList[i]) {
                    Row row = sheet.createRow(rowIndex++);
                    for (int c = 0; c < headers[i].length; c++) {
                        Cell cell = row.createCell(c);
                        Object value = c < data.length ? data[c] : null;
                        if (value instanceof Number) {
                            cell.setCellValue(((Number) value).doubleValue());
                        } else {
                            cell.setCellValue(value == null ? "" : value.toString());
                        }
                        cell.setCellStyle(bodyStyle);
                    }
                }
                for (int c = 0; c < headers[i].length; c++) {
                    sheet.setColumnWidth(c, Math.max(14, Math.min(38, headers[i][c].length() * 4 + 8)) * 256);
                }
                sheet.createFreezePane(0, 1);
            }
            try (FileOutputStream out = new FileOutputStream(file)) {
                wb.write(out);
            }
        }
    }

    private static CellStyle headStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontName("微软雅黑");
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        border(style);
        return style;
    }

    private static CellStyle bodyStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("微软雅黑");
        style.setFont(font);
        border(style);
        return style;
    }

    private static void border(CellStyle style) {
        style.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        style.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        style.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
        style.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);
    }
}
