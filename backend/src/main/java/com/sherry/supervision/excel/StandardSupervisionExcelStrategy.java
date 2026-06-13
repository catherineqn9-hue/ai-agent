package com.sherry.supervision.excel;

import com.fasterxml.jackson.databind.JsonNode;
import com.sherry.supervision.entity.ExcelImportTemplate;
import com.sherry.supervision.entity.SupervisionItem;
import com.sherry.supervision.exception.InvalidRequestException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

@Component
public class StandardSupervisionExcelStrategy implements ExcelImportStrategy {

    @Override
    public String handlerCode() {
        return "standard_supervision";
    }

    @Override
    public List<SupervisionItem> parse(InputStream inputStream, ExcelImportTemplate template) {
        try (var workbook = WorkbookFactory.create(inputStream)) {
            var sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);
            List<String> sourceColumns = jsonArrayToList(template.getSourceColumns());
            List<String> entityFields = jsonArrayToList(template.getEntityFields());
            List<SupervisionItem> items = new ArrayList<>();

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                SupervisionItem item = new SupervisionItem();
                item.setSourceRowNo(rowIndex + 1);
                for (int i = 0; i < sourceColumns.size() && i < entityFields.size(); i++) {
                    int columnIndex = findColumnIndex(header, sourceColumns.get(i));
                    if (columnIndex < 0) {
                        continue;
                    }
                    String value = row.getCell(columnIndex) == null
                            ? null
                            : row.getCell(columnIndex).toString().trim();
                    applyField(item, entityFields.get(i), value);
                }
                if (item.getTitle() != null && !item.getTitle().isBlank()) {
                    items.add(item);
                }
            }
            return items;
        } catch (Exception e) {
            throw new InvalidRequestException("Excel 文件解析失败，请检查文件格式和模板字段", e);
        }
    }

    private int findColumnIndex(Row header, String sourceColumn) {
        if (header == null) {
            return -1;
        }
        for (int i = 0; i < header.getLastCellNum(); i++) {
            if (header.getCell(i) != null && sourceColumn.equals(header.getCell(i).toString().trim())) {
                return i;
            }
        }
        return -1;
    }

    private List<String> jsonArrayToList(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return values;
        }
        Iterator<JsonNode> iterator = node.elements();
        while (iterator.hasNext()) {
            values.add(iterator.next().asText());
        }
        return values;
    }

    private void applyField(SupervisionItem item, String entityField, String value) {
        if ("item_no".equals(entityField)) {
            item.setItemNo(value);
        } else if ("title".equals(entityField)) {
            item.setTitle(value);
        } else if ("description".equals(entityField)) {
            item.setDescription(value);
        } else if ("priority".equals(entityField)) {
            item.setPriority(value);
        } else if ("status".equals(entityField)) {
            item.setStatus(value);
        } else if ("created_by".equals(entityField)) {
            item.setCreatedBy(value);
        }
    }
}
