package org.orman.sql;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.orman.sql.exception.NoTableSpecifiedException;
import org.orman.sql.exception.QueryBuilderException;
import org.orman.sql.util.Glue;
import org.orman.sql.util.Serializer;

public class QueryBuilder {

	/*
	 * BEGIN CHAINABLE METHODS 
	 */
	private Query query;

	private QueryBuilder(QueryType queryType) {
		query = new Query(queryType);
	}
	
	private QueryBuilder(Query query) {
		this.query = query;
	}

	public static QueryBuilder getBuilder(QueryType queryType) {
		return new QueryBuilder(queryType);
	}
	
	public static QueryBuilder getBuilder(Query existingQuery) {
		return new QueryBuilder(existingQuery);
	}
	
	public QueryBuilder from(String tableName) {
		query.addTable(tableName);
		return this;
	}
	
	public QueryBuilder fromAs(String tableName, String as) {
		query.addTable(tableName, as);
		return this;
	}
	
	public QueryBuilder from(String... tableNames) {
		for(String tbl : tableNames)
			this.from(tbl);
		return this;
	}
	
	public QueryBuilder from(Query q) {
		return this.fromAs(q, null);
	}
	
	public QueryBuilder fromAs(Query q, String as) {
		return this.fromAs(Serializer.serialize(q), as);
	}

	public QueryBuilder select(String... columns) {
		for (String col : columns)
			this.selectAs(col, null);
		return this;
	}
	
	public QueryBuilder select(Object... dataSources) {
		for (Object src : dataSources)
			this.selectAs(src.toString(), null);
		return this;
	}

	public QueryBuilder selectAs(String column, String as) {
		DataField df = new DataField(column, as);
		query.addField(df);

		return this;
	}
	
	/**
	 * Uses column list storage of the query, be cautious
	 * while using this except CREATE TABLE queries.
	 */
	public QueryBuilder createColumn(String column, String dataType){
		return this.selectAs(column, dataType);
	}
	

	public QueryBuilder sum(String field) {
		return this.sum(field, "sum");
	}

	public QueryBuilder sum(String field, String as) {
		return this.fieldOpAs(QueryFieldOperation.SUM, field, as);
	}
	
	public QueryBuilder min(String field) {
		return this.sum(field, "min");
	}
	
	public QueryBuilder min(String field, String as) {
		return this.fieldOpAs(QueryFieldOperation.MIN, field, as);
	}
	
	public QueryBuilder max(String field) {
		return this.sum(field, "max");
	}
	
	public QueryBuilder max(String field, String as) {
		return this.fieldOpAs(QueryFieldOperation.MAX, field, as);
	}

	public QueryBuilder count() {
		return this.count("*"); // ALL
	}

	public QueryBuilder count(String column) {
		return this.countAs(column, "count");
	}

	public QueryBuilder countAs(String column, String as) {
		return this.fieldOpAs(QueryFieldOperation.COUNT, column, as);
	}

	public QueryBuilder fieldOp(QueryFieldOperation op, String column){
		return fieldOpAs(op, column, null);
	}
	
	public QueryBuilder fieldOpAs(QueryFieldOperation op, String column,
			String as) {
		OperationalField opf = new OperationalField(op, column, as);
		query.addField(opf);
		return this;
	}
	
	/**
	 * Uses field list and table list of the query to hold index fields,
	 * be cautious while using this except CREATE [UNIQUE] index queries.
	 * 
	 * Index name STORED ON => field alias
	 * Index column STORED ON => field list
	 * Table name STORED ON => table name (use .from())
	 */
	public QueryBuilder setIndex(String on, String indexName){
		this.query.setIndexName(indexName);
		return this.select(on);
	}

	public QueryBuilder set(String field, int value){
		return this.setField(field, new Integer(value));
	}
	
	public QueryBuilder set(String field, float value){
		return this.setField(field, new Float(value));
	}
	
	public QueryBuilder set(String field, double value){
		return this.setField(field, new Double(value));
	}
	
	public QueryBuilder set(String field, Query value){
		return this.setField(field, value);
	}
	
	public QueryBuilder set(String field, String value){
		return this.setField(field, value);
	}
	
	public QueryBuilder set(String field, Object value){
		return this.setField(field, value);
	}
	
	private QueryBuilder setField(String field, Object value){
		query.setField(field, value);
		return this;
	}
	
	public QueryBuilder limit(int recordCount){
		return this.limit(recordCount, 0);
	}
	
	public QueryBuilder limit(int recordCount, int startOffset) {
		ISubclause s = new LimitClause(recordCount, startOffset);
		query.addSubclause(SubclauseType.LIMIT, s);
		return this;
	}
	
	
	public QueryBuilder groupBy(String... by){
		ISubclause s = new GroupByClause(by);
		query.addSubclause(SubclauseType.GROUP_BY, s);
		return this;
	}
	
	public QueryBuilder having(Criterion c){
		ISubclause s = new HavingClause(c);
		query.addSubclause(SubclauseType.HAVING, s);
		return this;
	}
	
	public QueryBuilder where(Criterion c){
		ISubclause s = new WhereClause(c);
		query.addSubclause(SubclauseType.WHERE, s);
		return this;
	}

	public QueryBuilder orderBy(String... fields){
		ISubclause s = new OrderByClause(fields);
		query.addSubclause(SubclauseType.ORDER_BY, s);
		return this;
	}
	
	public QueryBuilder join(String table){
		return join(JoinType.JOIN, table, null);
	}
	
	public QueryBuilder join(String table, Criterion on){
		return join(JoinType.JOIN, table, on);
	}
	
	public QueryBuilder join(JoinType type, String table, Criterion on){
		ISubclause s = new JoinClause(type, table, on);
		query.addSubclause(SubclauseType.JOIN, s);
		return this;
	}
	
	/*
	 * END CHAINABLE METHODS 
	 */
	
	
	/*
	 * START HELPER METHODS
	 */
	public Query getQuery(){
		return this.query;
	}
	
	public Query union(Query... queries){
		return multiQueryOp(MultiQuerySetOp.UNION, queries);
	}
	
	public Query unionAll(Query... queries){
		return multiQueryOp(MultiQuerySetOp.UNION_ALL, queries);
	}
	
	public Query intersect(Query... queries){
		return multiQueryOp(MultiQuerySetOp.INTERSECT, queries);
	}
	
	public Query intersectAll(Query... queries){
		return multiQueryOp(MultiQuerySetOp.INTERSECT_ALL, queries);
	}
	
	public Query except(Query... queries){
		return multiQueryOp(MultiQuerySetOp.EXCEPT, queries);
	}
	
	public Query exceptAll(Query... queries){
		return multiQueryOp(MultiQuerySetOp.EXCEPT_ALL, queries);
	}
	
	
	
	private Query multiQueryOp(MultiQuerySetOp op, Query... queries){
		Query[] qs = new Query[queries.length+1];
		qs[0] = this.query; // first query to concat is this->query
		for(int i = 1; i < qs.length; i++){
			qs[i] = queries[i-1];
		}
		
		return new MultipleQuery(op, qs);
	}

	
	
	/*
	 * QUERY BUILDER METHODS 
	 */
	
	private String prepareSelectFieldList() {
		List<IQueryField> fields = query.getFieldList();
		
		if(fields == null || fields.isEmpty()){
			return "*";
		} else {
			return Glue.concat(query.getFieldList(), ", ");
		}
	}
	
	private String prepareValueList() {
		Set<Entry<String, String>> e = query.getValuedFieldMap().entrySet();
		
		StringBuilder sb = new StringBuilder();
		int i = 0;
		for(Entry<String, String> r : e){
			sb.append(r.getValue());
			if(i++ != e.size()) sb.append(", ");
		}
		return sb.toString();
	}

	private String prepareValuedFieldList() {
		Set<Entry<String, String>> e = query.getValuedFieldMap().entrySet();
		
		StringBuilder sb = new StringBuilder();
		int i = 0;
		for(Entry<String, String> r : e){
			sb.append(r.getKey());
			if(i++ != e.size()) sb.append(", ");
		}
		return sb.toString();
	}

	private String prepareFieldValuePairList() {
		Set<Entry<String, String>> e = query.getValuedFieldMap().entrySet();
		
		StringBuilder sb = new StringBuilder();
		int i = 0;
		for(Entry<String, String> r : e){
			sb.append(r.getKey()+"="+r.getValue());
			
			if(i++ != e.size()) sb.append(", ");
		}
		return sb.toString();
	}

	private String prepareTableList() throws NoTableSpecifiedException {
		List<Table> tables = query.getTables();
		
		if((tables == null || tables.isEmpty()))
			throw new NoTableSpecifiedException();
		
		return Glue.concat(tables, ", ");
	}

	
	private String prepareDatabase() {
		return query.getDatabase();
	}

	private static Set<String> extractFields(String template) {
		Set<String> fields = new HashSet<String>();

		int s = -1, e = 0;
		while (s < template.length()) {
			s = template.indexOf('{', s);
			e = template.indexOf('}', s);
			if (s > -1 && e > 0) {
				fields.add(template.substring(s + 1, e));
			} else
				break;
			s = e;
		}

		return fields;
	}
	
	private static String fillTemplate(String template, Map<String, String> modelMap){
		for(Entry<String, String> modelEntry : modelMap.entrySet()){
			template = template.replace("{"+modelEntry.getKey()+"}", modelEntry.getValue());
		}
		return template;
	}
	
	/**
	 * using DataField objects, build e.g.:
	 * id INTEGER, name VARCHAR(100) 
	 */
	private String prepareFieldDescriptionList() {
		List<IQueryField> fl = this.query.getFieldList();
		
		StringBuffer sb = new StringBuffer();
		for(int i = 0; i < fl.size(); i++){
			sb.append(fl.get(i).getFieldName());
			sb.append(' ');
			sb.append(fl.get(i).getAlias());
			
			if(i != fl.size()-1) sb.append(", ");
		}
		
		return sb.toString();
	}
	
	private String prepareIndexName() {
		return this.query.getIndexName();
	}

	private String getTemplateFieldValue(String tplField) throws QueryBuilderException {
		if ("SELECT_COLUMN_LIST".equals(tplField))
			return prepareSelectFieldList();
		if ("COLUMN_DESCRIPTION_LIST".equals(tplField))
			return prepareFieldDescriptionList();
		if ("TABLE_LIST".equals(tplField))
			return prepareTableList();
		if ("INDEX_NAME".equals(tplField))
			return prepareIndexName();
		if ("VALUE_LIST".equals(tplField))
			return prepareValueList();
		if ("COLUMN_VALUE_LIST".equals(tplField))
			return prepareFieldValuePairList();
		if ("COLUMN_LIST".equals(tplField))
			return prepareValuedFieldList();
		if ("DATABASE".equals(tplField))
			return prepareDatabase();
		
		// if reserved placeholder keywords are not found
		// try to match subclauses e.g. WHERE, GROUP_BY
		SubclauseType subclause = SubclauseType.lookup(tplField);
		if(subclause != null){
			return this.prepareSubclause(subclause);
		}
		
		// nothing found, desperately replace it with ""
		return "";
	}
	

	private String prepareSubclause(SubclauseType sType) {
		ISubclause s = this.query.getSubclause(sType);
		
		if (s != null){
			return " "+ s.toString(); // prepend a space to split subclauses {A}{B}->AVAL BVAL
		} else {
			return "";
		}
	}
	
	public String prepareSql() {// StringBuffer usage
		String template = query.getType().getTemplate(); 
		Set<String> dynamicFields = extractFields(template);
		
		Map<String,String> modelMap = new HashMap<String, String>();
		for(String tplField : dynamicFields){
			modelMap.put(tplField, getTemplateFieldValue(tplField));;
		}
		
		template = fillTemplate(template, modelMap);
		
		return template;
	}
	
	/*
	 * END QUERY BUILDER METHODS 
	 */
	
}