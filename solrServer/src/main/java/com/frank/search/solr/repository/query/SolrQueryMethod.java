/*
 * Copyright 2012 - 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.frank.search.solr.repository.query;

import com.frank.search.solr.core.query.Query;
import com.frank.search.solr.repository.*;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Solr specific implementation of {@link org.springframework.data.repository.query.QueryMethod} taking care of {@link com.frank.search.solr.repository.Query}
 * 
 * @author Christoph Strobl
 * @author Luke Corpe
 * @author Andrey Paramonov
 * @author Francisco Spaeth
 */
public class SolrQueryMethod extends QueryMethod {

	private Method method;

	public SolrQueryMethod(Method method, RepositoryMetadata metadata, SolrEntityInformationCreator solrInformationCreator) {
		super(method, metadata);
		this.method = method;
	}

	/**
	 * @return true if {@link com.frank.search.solr.repository.Query#value()} is not blank
	 */
	public boolean hasAnnotatedQuery() {
		return getAnnotatedQuery() != null;
	}

	/**
	 * @return true if {@link com.frank.search.solr.repository.Query} is not blank
	 */
	public boolean hasQueryAnnotation() {
		return this.method.getAnnotation(com.frank.search.solr.repository.Query.class) != null;
	}

	String getAnnotatedQuery() {
		return getAnnotationValueAsStringOrNullIfBlank(getQueryAnnotation(), "value");
	}

	/**
	 * @return true if {@link com.frank.search.solr.repository.Query#name()} is not blank
	 */
	public boolean hasAnnotatedNamedQueryName() {
		return getAnnotatedNamedQueryName() != null;
	}

	String getAnnotatedNamedQueryName() {
		return getAnnotationValueAsStringOrNullIfBlank(getQueryAnnotation(), "name");
	}

	private com.frank.search.solr.repository.Query getQueryAnnotation() {
		return this.method.getAnnotation(com.frank.search.solr.repository.Query.class);
	}

	TypeInformation<?> getReturnType() {
		return ClassTypeInformation.fromReturnTypeOf(method);
	}

	/**
	 * @return true if {@link com.frank.search.solr.repository.Query#fields()} is not empty
	 */
	public boolean hasProjectionFields() {
		if (hasQueryAnnotation()) {
			return !CollectionUtils.isEmpty(getProjectionFields());
		}
		return false;
	}

	/**
	 * @return empty collection if {@link com.frank.search.solr.repository.Query#fields()} is empty
	 */
	public List<String> getProjectionFields() {
		return getAnnotationValuesAsStringList(getQueryAnnotation(), "fields");
	}

	/**
	 * @return null if {@link com.frank.search.solr.repository.Query#timeAllowed()} is null or negative
	 */
	public Integer getTimeAllowed() {
		if (hasQueryAnnotation()) {
			return getAnnotationValueAsIntOrNullIfNegative(getQueryAnnotation(), "timeAllowed");
		}
		return null;
	}

	/**
	 * @return true if {@link #hasFacetFields()} or {@link #hasFacetQueries()}
	 */
	public boolean isFacetQuery() {
		return hasFacetFields() || hasFacetQueries() || hasPivotFields();
	}

	/**
	 * @return true if {@link Facet#fields()} is not empty
	 */
	public boolean hasFacetFields() {
		if (hasFacetAnnotation()) {
			return !CollectionUtils.isEmpty(getFacetFields());
		}
		return false;
	}

	/**
	 * @return true if {@link Facet#pivotFields()} is not empty
	 */
	public boolean hasPivotFields() {
		if (hasFacetAnnotation()) {
			return !CollectionUtils.isEmpty(getPivotFields());
		}
		return false;
	}

	private boolean hasFacetAnnotation() {
		return getFacetAnnotation() != null;
	}

	/**
	 * @return empty collection if {@link Facet#fields()} is empty
	 */
	public List<String> getFacetFields() {
		return getAnnotationValuesAsStringList(getFacetAnnotation(), "fields");
	}

	/**
	 * @return empty collection if {@link Facet#queries()} is empty
	 */
	public List<String> getFacetQueries() {
		return getAnnotationValuesAsStringList(getFacetAnnotation(), "queries");
	}

	public List<String[]> getPivotFields() {
		List<Pivot> pivotFields = getAnnotationValuesList(getFacetAnnotation(), "pivots", Pivot.class);
		ArrayList<String[]> result = new ArrayList<String[]>();

		for (Pivot pivot : pivotFields) {
			result.add(pivot.value());
		}

		return result;
	}

	/**
	 * @return true if {@link Facet#queries()} is not empty
	 */
	public boolean hasFacetQueries() {
		if (hasFacetAnnotation()) {
			return !CollectionUtils.isEmpty(getFacetQueries());
		}
		return false;
	}

	private Facet getFacetAnnotation() {
		return this.method.getAnnotation(Facet.class);
	}

	/**
	 * @return value of {@link Facet#limit()}
	 */
	public Integer getFacetLimit() {
		return (Integer) AnnotationUtils.getValue(getFacetAnnotation(), "limit");
	}

	/**
	 * @return value of {@link Facet#minCount()}
	 */
	public Integer getFacetMinCount() {
		return (Integer) AnnotationUtils.getValue(getFacetAnnotation(), "minCount");
	}

	/**
	 * @return value of {@link Facet#prefix()}
	 */
	public String getFacetPrefix() {
		return getAnnotationValueAsStringOrNullIfBlank(getFacetAnnotation(), "prefix");
	}

	/**
	 * @return the {@link Stats} annotation, null if there is none
	 */
	private Stats getStatsAnnotation() {
		return this.method.getAnnotation(Stats.class);
	}

	/**
	 * @return if something was configured within {@link Stats}
	 * @since 1.4
	 */
	public boolean hasStatsDefinition() {
		return (//
		!(getStatsAnnotation() == null) && (//
		!getFieldStats().isEmpty() || //
				!getStatsFacets().isEmpty() || //
				!getStatsSelectiveFacets().isEmpty() || //
		!getStatsSelectiveCountDistinctFields().isEmpty())//
		);
	}

	/**
	 * @return true if stats is distinct
	 * @since 1.4
	 */
	public boolean isFieldStatsCountDistinctEnable() {

		Stats stats = getStatsAnnotation();
		return stats != null && stats.distinct();
	}

	/**
	 * @return value of {@link Stats#value()}
	 * @since 1.4
	 */
	public List<String> getFieldStats() {
		return getAnnotationValuesAsStringList(getStatsAnnotation(), "value");
	}

	/**
	 * @return value of {@link Stats#facets()}
	 * @since 1.4
	 */
	public List<String> getStatsFacets() {
		return getAnnotationValuesAsStringList(getStatsAnnotation(), "facets");
	}

	/**
	 * @return value of facets used in {@link Stats#selective()}
	 * @since 1.4
	 */
	public Map<String, String[]> getStatsSelectiveFacets() {

		List<SelectiveStats> selective = getAnnotationValuesList(getStatsAnnotation(), "selective", SelectiveStats.class);

		Map<String, String[]> result = new LinkedHashMap<String, String[]>();
		for (SelectiveStats selectiveFacet : selective) {
			result.put(selectiveFacet.field(), selectiveFacet.facets());
		}

		return result;
	}

	/**
	 * @return value of facets used in {@link Stats#selective()}
	 * @since 1.4
	 */
	public Collection<String> getStatsSelectiveCountDistinctFields() {

		List<SelectiveStats> selective = getAnnotationValuesList(getStatsAnnotation(), "selective", SelectiveStats.class);

		Collection<String> result = new LinkedHashSet<String>();
		for (SelectiveStats selectiveFacet : selective) {
			if (selectiveFacet.distinct()) {
				result.add(selectiveFacet.field());
			}
		}

		return result;
	}

	/**
	 * @return true if {@link com.frank.search.solr.repository.Query#filters()} is not empty
	 */
	public boolean hasFilterQuery() {
		if (hasQueryAnnotation()) {
			return !CollectionUtils.isEmpty(getFilterQueries());
		}
		return false;
	}

	/**
	 * @return value of {@link com.frank.search.solr.repository.Query#delete()}
	 * @since 1.2
	 */
	public boolean isDeleteQuery() {
		if (hasQueryAnnotation()) {
			return ((Boolean) AnnotationUtils.getValue(getQueryAnnotation(), "delete")).booleanValue();
		}
		return false;
	}

	private Annotation getHighlightAnnotation() {
		return this.method.getAnnotation(Highlight.class);
	}

	private boolean hasHighlightAnnotation() {
		return this.getHighlightAnnotation() != null;
	}

	/**
	 * @return if {@link Highlight} is present
	 */
	public boolean isHighlightQuery() {
		return this.hasHighlightAnnotation();
	}

	/**
	 * @return empty collection if {@link Highlight#fields()} is empty
	 */
	public List<String> getHighlightFieldNames() {
		if (hasHighlightAnnotation()) {
			return this.getAnnotationValuesAsStringList(getHighlightAnnotation(), "fields");
		}
		return Collections.emptyList();
	}

	/**
	 * @return null if {@link Highlight#query()} is blank
	 */
	public String getHighlightQuery() {
		if (hasHighlightAnnotation()) {
			return getAnnotationValueAsStringOrNullIfBlank(getHighlightAnnotation(), "query");
		}
		return null;
	}

	/**
	 * @return value of {@link Highlight#snipplets()} or null if negative
	 */
	public Integer getHighlighSnipplets() {
		if (hasHighlightAnnotation()) {
			return getAnnotationValueAsIntOrNullIfNegative(getHighlightAnnotation(), "snipplets");
		}
		return null;
	}

	/**
	 * @return value of {@link Highlight#fragsize()} or null if negative
	 */
	public Integer getHighlightFragsize() {
		if (hasHighlightAnnotation()) {
			return getAnnotationValueAsIntOrNullIfNegative(getHighlightAnnotation(), "fragsize");
		}
		return null;
	}

	/**
	 * @return value of {@link Highlight#formatter()} or null if blank
	 */
	public String getHighlightFormatter() {
		if (hasHighlightAnnotation()) {
			return getAnnotationValueAsStringOrNullIfBlank(getHighlightAnnotation(), "formatter");
		}
		return null;
	}

	/**
	 * @return value of {@link Highlight#prefix()} or null if blank
	 */
	public String getHighlightPrefix() {
		if (hasHighlightAnnotation()) {
			return getAnnotationValueAsStringOrNullIfBlank(getHighlightAnnotation(), "prefix");
		}
		return null;
	}

	/**
	 * @return value of {@link Highlight#postfix()} or null if blank
	 */
	public String getHighlightPostfix() {
		if (hasHighlightAnnotation()) {
			return getAnnotationValueAsStringOrNullIfBlank(getHighlightAnnotation(), "postfix");
		}
		return null;
	}

	/**
	 * @return true if {@link Highlight#fields()} is not empty
	 */
	public boolean hasHighlightFields() {
		return !getHighlightFieldNames().isEmpty();
	}

	List<String> getFilterQueries() {
		return getAnnotationValuesAsStringList(getQueryAnnotation(), "filters");
	}

	/**
	 * @return value of {@link com.frank.search.solr.repository.Query#defaultOperator()} or
	 *         {@link org.springframework.data.solr.core.query.Query.Operator#NONE} if not set
	 */
	public Query.Operator getDefaultOperator() {
		if (hasQueryAnnotation()) {
			return getQueryAnnotation().defaultOperator();
		}
		return Query.Operator.NONE;
	}

	/**
	 * @return null if {@link com.frank.search.solr.repository.Query#defType()} not set
	 */
	public String getDefType() {
		if (hasQueryAnnotation()) {
			return getQueryAnnotation().defType();
		}
		return null;
	}

	/**
	 * @return null if {@link com.frank.search.solr.repository.Query#requestHandler()} not set
	 */
	public String getRequestHandler() {
		if (hasQueryAnnotation()) {
			return getQueryAnnotation().requestHandler();
		}
		return null;
	}

	private String getAnnotationValueAsStringOrNullIfBlank(Annotation annotation, String attributeName) {
		String value = (String) AnnotationUtils.getValue(annotation, attributeName);
		return StringUtils.hasText(value) ? value : null;
	}

	private Integer getAnnotationValueAsIntOrNullIfNegative(Annotation annotation, String attributeName) {
		Integer timeAllowed = (Integer) AnnotationUtils.getValue(annotation, attributeName);
		if (timeAllowed != null && timeAllowed.intValue() > 0) {
			return timeAllowed;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private List<String> getAnnotationValuesAsStringList(Annotation annotation, String attribute) {
		String[] values = (String[]) AnnotationUtils.getValue(annotation, attribute);
		if (values.length > 1 || (values.length == 1 && StringUtils.hasText(values[0]))) {
			return CollectionUtils.arrayToList(values);
		}
		return Collections.emptyList();
	}

	@SuppressWarnings("unchecked")
	private <T> List<T> getAnnotationValuesList(Annotation annotation, String attribute, Class<T> clazz) {
		T[] values = (T[]) AnnotationUtils.getValue(annotation, attribute);
		return CollectionUtils.arrayToList(values);
	}

	@Override
	public String getNamedQueryName() {
		if (!hasAnnotatedNamedQueryName()) {
			return super.getNamedQueryName();
		}
		return getAnnotatedNamedQueryName();
	}

	@Override
	protected SolrParameters createParameters(Method method) {
		return new SolrParameters(method);
	}

	@Override
	public SolrParameters getParameters() {
		return (SolrParameters) super.getParameters();
	}

}
