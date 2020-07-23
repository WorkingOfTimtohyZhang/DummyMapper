package io.goodforgod.dummymapper.mapper.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.intellij.psi.PsiJavaFile;
import io.dummymaker.factory.impl.GenFactory;
import io.goodforgod.dummymapper.error.ParseException;
import io.goodforgod.dummymapper.filter.IFilter;
import io.goodforgod.dummymapper.filter.impl.ExcludeSetterAnnotationFilter;
import io.goodforgod.dummymapper.mapper.IMapper;
import io.goodforgod.dummymapper.marker.Marker;
import io.goodforgod.dummymapper.marker.RawMarker;
import io.goodforgod.dummymapper.service.ClassFactory;
import io.goodforgod.dummymapper.service.GenFactoryProvider;
import io.goodforgod.dummymapper.service.PsiJavaFileScanner;
import io.goodforgod.dummymapper.ui.config.JsonArrayConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

/**
 * Maps instance of {@link PsiJavaFile} to JSON format as example
 *
 * @author Anton Kurako (GoodforGod)
 * @since 28.4.2020
 */
@SuppressWarnings("DuplicatedCode")
public class JsonArrayMapper implements IMapper<JsonArrayConfig> {

    private final ObjectMapper mapper;
    private final IFilter annotationFilter;

    public JsonArrayMapper() {
        this.annotationFilter = new ExcludeSetterAnnotationFilter();
        this.mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        this.mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX"));
    }

    @NotNull
    @Override
    public String map(@NotNull RawMarker marker) {
        return map(marker, null);
    }

    @NotNull
    @Override
    public String map(@NotNull RawMarker marker, @Nullable JsonArrayConfig config) {
        try {
            if (marker.isEmpty())
                return "";

            final RawMarker filtered = annotationFilter.filter(marker);
            final Map<String, Marker> structure = filtered.getStructure();
            final Class<?> target = ClassFactory.build(structure);

            final GenFactory factory = GenFactoryProvider.get(structure);

            final int amount = (config == null) ? 1 : config.getAmount();
            final List<?> list = factory.build(target, amount);

            return mapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            throw new ParseException(e.getMessage(), e);
        }
    }
}
