package com.course.sims.util;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel 导入导出工具类（基于 Apache POI）。
 *
 * <p>统一了导出样式（表头加粗居中、灰底、全表细边框、自动列宽、冻结首行），
 * 使各业务模块只需提供表头与数据行，无需重复编写 POI 样板代码。</p>
 *
 * @author 课程设计第 2 小组 - 成员 E
 * @version 1.0.0
 */
public final class ExcelUtil {

    private ExcelUtil() {
    }

    /**
     * 导出数据到 Excel 文件。
     *
     * @param file    目标文件
     * @param sheetName 工作表名称
     * @param title   报表标题（合并居中显示，可为 null）
     * @param headers 表头数组
     * @param rows    数据行，每行为一个 Object 数组
     * @throws IOException 写入失败
     */
    public static void export(File file, String sheetName, String title, String[] headers, List<Object[]> rows)
            throws IOException {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(sheetName == null || sheetName.isBlank() ? "Sheet1" : sheetName);
            CellStyle titleStyle = buildTitleStyle(wb);
            CellStyle headStyle = buildHeadStyle(wb);
            CellStyle bodyStyle = buildBodyStyle(wb);

            int rowIndex = 0;
            if (title != null && !title.isBlank()) {
                Row titleRow = sheet.createRow(rowIndex++);
                titleRow.setHeightInPoints(24);
                Cell cell = titleRow.createCell(0);
                cell.setCellValue(title);
                cell.setCellStyle(titleStyle);
                if (headers.length > 1) {
                    sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, headers.length - 1));
                }
            }

            Row headRow = sheet.createRow(rowIndex++);
            headRow.setHeightInPoints(20);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headStyle);
            }

            for (Object[] data : rows) {
                Row row = sheet.createRow(rowIndex++);
                for (int i = 0; i < headers.length; i++) {
                    Cell cell = row.createCell(i);
                    Object value = i < data.length ? data[i] : null;
                    if (value instanceof Number) {
                        cell.setCellValue(((Number) value).doubleValue());
                    } else {
                        cell.setCellValue(value == null ? "" : value.toString());
                    }
                    cell.setCellStyle(bodyStyle);
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, Math.max(12, Math.min(40, headers[i].length() * 4 + 8)) * 256);
            }
            sheet.createFreezePane(0, title == null || title.isBlank() ? 1 : 2);

            try (FileOutputStream out = new FileOutputStream(file)) {
                wb.write(out);
            }
        }
    }

    /**
     * 读取 Excel 文件全部数据行。
     *
     * <p>本系统的导出文件第一行是合并的报表标题、第二行才是表头，
     * 而手工准备或从别处拿到的文件可能没有标题行。因此本方法不依赖固定行号，
     * 而是自动跳过"表头之前的所有行"：一旦遇到包含至少两个非空单元格的行
     * （即真正的表头行）即从下一行开始读取数据，从而对两种情况都适用。</p>
     *
     * @param file       Excel 文件（.xlsx / .xls）
     * @param skipHeader 是否识别并跳过表头行（true 为自动识别）
     * @return 数据行，每行按列顺序返回字符串数组
     * @throws IOException 读取失败
     */
    public static List<String[]> read(File file, boolean skipHeader) throws IOException {
        List<String[]> rows = new ArrayList<>();
        try (InputStream in = new FileInputStream(file); Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheetAt(0);
            int lastRow = sheet.getLastRowNum();
            boolean headerFound = !skipHeader;
            for (int i = 0; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                int lastCell = Math.max(row.getLastCellNum(), 1);
                String[] values = new String[lastCell];
                boolean empty = true;
                int nonEmpty = 0;
                for (int j = 0; j < lastCell; j++) {
                    values[j] = readCell(row.getCell(j));
                    if (values[j] != null && !values[j].isBlank()) {
                        empty = false;
                        nonEmpty++;
                    }
                }
                if (empty) {
                    continue;
                }
                if (!headerFound) {
                    if (nonEmpty >= 2) {
                        headerFound = true;
                    }
                    continue;
                }
                rows.add(values);
            }
        }
        return rows;
    }

    /**
     * 读取单元格文本，数值类型避免出现 "88.0" 这类多余小数。
     *
     * @param cell 单元格
     * @return 文本内容
     */
    private static String readCell(Cell cell) {
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case NUMERIC:
                double d = cell.getNumericCellValue();
                if (d == Math.floor(d) && !Double.isInfinite(d)) {
                    return String.valueOf((long) d);
                }
                return String.valueOf(d);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return String.valueOf(cell.getNumericCellValue());
                } catch (IllegalStateException e) {
                    return cell.getStringCellValue();
                }
            default:
                return cell.getStringCellValue() == null ? "" : cell.getStringCellValue().trim();
        }
    }

    /**
     * 构造报表标题样式。
     *
     * @param wb 工作簿
     * @return 样式
     */
    private static CellStyle buildTitleStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        font.setFontName("微软雅黑");
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    /**
     * 构造表头样式。
     *
     * @param wb 工作簿
     * @return 样式
     */
    private static CellStyle buildHeadStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontName("微软雅黑");
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        setBorder(style);
        return style;
    }

    /**
     * 构造数据行样式。
     *
     * @param wb 工作簿
     * @return 样式
     */
    private static CellStyle buildBodyStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setFontName("微软雅黑");
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(style);
        return style;
    }

    /**
     * 设置四边细边框。
     *
     * @param style 样式
     */
    private static void setBorder(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}
