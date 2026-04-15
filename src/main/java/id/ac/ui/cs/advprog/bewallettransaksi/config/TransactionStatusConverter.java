package id.ac.ui.cs.advprog.bewallettransaksi.config;

import id.ac.ui.cs.advprog.bewallettransaksi.enums.TransactionStatus;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class TransactionStatusConverter implements Converter<String, TransactionStatus> {

    @Override
    public TransactionStatus convert(String source) {
        if (source == null) {
            return null;
        }
        return TransactionStatus.valueOf(source.trim().toUpperCase());
    }
}
