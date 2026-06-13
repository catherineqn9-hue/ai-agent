package com.sherry.supervision.excel;

import com.sherry.supervision.entity.ExcelImportTemplate;
import com.sherry.supervision.entity.SupervisionItem;
import java.io.InputStream;
import java.util.List;

public interface ExcelImportStrategy {

    String handlerCode();

    List<SupervisionItem> parse(InputStream inputStream, ExcelImportTemplate template);
}
