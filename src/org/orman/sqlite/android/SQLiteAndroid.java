package org.orman.sqlite.android;

import org.orman.datasource.DataTypeMapper;
import org.orman.datasource.Database;
import org.orman.datasource.QueryExecutionContainer;
import org.orman.sql.SQLGrammarProvider;
import org.orman.sqlite.DataTypeMapperImpl;
import org.orman.sqlite.SQLiteGrammar;
import org.orman.util.Log;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * SQLite wrapper for Android SDK <code>android.database.sqlite
 * </code> package. Uses Android SDK 3 as a reference, for high
 * compatibility.
 * 
 * Shares same grammar and data type mapper with <code>org.orman.sqlite</code>
 * package.
 * 
 * @author ahmet alp balkan <ahmetalpbalkan at gmail.com>
 */
public class SQLiteAndroid extends SQLiteOpenHelper implements Database {
	private static final int SQLITE_VERSION = 33; //TODO read from somewhere else ASAP
	private DataTypeMapper typeMapper;
	private QueryExecutionContainerImpl executer;
	private SQLiteGrammar grammar;
	
	private String databaseName;
	private SQLiteDatabase db;
	
	public SQLiteAndroid(Context context, String dbFilename) {
		super(context, dbFilename, null, SQLITE_VERSION);
		SQLiteDatabase db = getWritableDatabase();
		this.db = db;
		
		this.databaseName = dbFilename;
		typeMapper = new DataTypeMapperImpl();
		executer = new QueryExecutionContainerImpl(this.db); //bind database onCreate.
		grammar = new SQLiteGrammar();
		
		Log.trace("Orman: DB initialized at %s", this.db.getPath());
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		//TODO currently defers creation of database to SchemeCreationPolicy.
		// and does not create database here.
		Log.warn("Orman: SQLite database onCreate invoked. Database path %s.", db.getPath());
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.warn("onUpgrade");
		Log.info(
				"Orman: SQLite database onUpgrade invoked. Database path %s. Old %d, new %d.",
				db.getPath(), oldVersion, newVersion);
	}

	@Override
	public QueryExecutionContainer getExecuter() {
		return executer;
	}

	@Override
	public DataTypeMapper getTypeMapper() {
		return typeMapper;
	}

	@Override
	public void closeConnection() {
		executer.close();
		Log.info("Connection terminated successfully.");
	}

	@Override
	public SQLGrammarProvider getSQLGrammar() {
		return grammar;
	}

	
	public String getDatabaseName() {
		return databaseName;
	}

}
