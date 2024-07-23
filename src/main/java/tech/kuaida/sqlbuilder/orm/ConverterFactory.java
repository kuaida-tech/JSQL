package tech.kuaida.sqlbuilder.orm;

/**
 * Factory for returning a converter given a Java field type.
 */
public interface ConverterFactory {

    public Converter<?> getConverter(Class<?> fieldClass);

}
