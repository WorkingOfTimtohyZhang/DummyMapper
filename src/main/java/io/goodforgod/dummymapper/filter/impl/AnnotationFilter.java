package io.goodforgod.dummymapper.filter.impl;

import io.goodforgod.dummymapper.marker.Marker;
import io.goodforgod.dummymapper.marker.RawMarker;
import io.goodforgod.dummymapper.model.AnnotationMarker;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Filters out {@link AnnotationMarker} from {@link Marker} which are not qualified to {@link #predicate()}
 *
 * @author Anton Kurako (GoodforGod)
 * @since 1.5.2020
 */
public abstract class AnnotationFilter extends BaseFilter {

    /**
     * @return predicate for annotations that will be allowed
     */
    protected abstract Predicate<AnnotationMarker> predicate();

    @NotNull
    @Override
    public RawMarker filter(@NotNull RawMarker marker) {
        final Predicate<AnnotationMarker> predicate = predicate();

        final Map<String, Marker> structure = marker.getStructure();
        structure.forEach((k, v) -> {
            final Set<AnnotationMarker> allowed = v.getAnnotations().stream()
                    .filter(predicate)
                    .collect(Collectors.toSet());
            v.setAnnotations(allowed);
        });

        return filterRecursive(marker);
    }
}
