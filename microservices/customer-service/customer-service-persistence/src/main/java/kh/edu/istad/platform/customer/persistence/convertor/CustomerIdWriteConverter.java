package kh.edu.istad.platform.customer.persistence.convertor;

import kh.edu.istad.common.domain.valueobject.CustomerId;
import org.bson.Document;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

@WritingConverter
public class CustomerIdWriteConverter implements Converter<CustomerId, Document> {

    @Override
    public Document convert(CustomerId source) {
        return new Document("_id",source.value().toString());
    }
}
