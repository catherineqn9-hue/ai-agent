package com.sherry.supervision.excel;

import com.sherry.supervision.exception.InvalidRequestException;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ExcelImportStrategyFactory {

    private final Map<String, ExcelImportStrategy> strategies;

    public ExcelImportStrategyFactory(java.util.List<ExcelImportStrategy> strategies) {
        this.strategies = strategies.stream()
                .collect(Collectors.toMap(ExcelImportStrategy::handlerCode, Function.identity()));
    }

    public ExcelImportStrategy get(String handlerCode) {
        ExcelImportStrategy strategy = strategies.get(handlerCode);
        if (strategy == null) {
            throw new InvalidRequestException("Excel 导入策略不存在：" + handlerCode);
        }
        return strategy;
    }
}
