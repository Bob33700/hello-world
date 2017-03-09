package com.bluegecko.sos.database;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

import static com.bluegecko.sos.utils.Resources.formatPhoneNumber;
/**
 * Created by Bob on 13/05/2016
 */


public class Recipients {
	private Context mContext;
	public int size;

	public Recipients(Context context) {
		mContext = context;
		size = getAllRows().size();
    }
	
	public static final String RECIPIENTS_ID = DataProvider.RECIPIENTS_ID;
	public static final String RECIPIENTS_RANK = DataProvider.RECIPIENTS_RANK;
	public static final String RECIPIENTS_PHONE = DataProvider.RECIPIENTS_PHONE;
	public static final String RECIPIENTS_NAME = DataProvider.RECIPIENTS_NAME;
	public static final String RECIPIENTS_MESSAGE = DataProvider.RECIPIENTS_MESSAGE;
	public static final String RECIPIENTS_STATUS = DataProvider.RECIPIENTS_STATUS;
	public static final String RECIPIENTS_POSITION = DataProvider.RECIPIENTS_POSITION;
	public static final String RECIPIENTS_CALLING = DataProvider.RECIPIENTS_CALLING;


	public int getMaxRank(){
		return size-1;
	}
	public int getNewRank(){
		return size;
	}

	// ajouter une ligne
	public long addRecipient(Recipient r){
		ContentValues values = new ContentValues();
		values.put(RECIPIENTS_RANK, getNewRank());
		values.put(RECIPIENTS_PHONE, r.getPhone());
		values.put(RECIPIENTS_NAME, r.getName());
		values.put(RECIPIENTS_MESSAGE, r.getMessage());
		values.put(RECIPIENTS_STATUS, r.getStatus());
		values.put(RECIPIENTS_POSITION, r.getPosition());
		values.put(RECIPIENTS_CALLING, r.getCalling());
		ContentResolver content = mContext.getContentResolver();
		Uri uri = content.insert(Uri.parse(DataProvider.RECIPIENTS_URL), values);
		if (uri != null) {
			size++;
			return Long.parseLong(uri.getPathSegments().get(1));
		} else {
			return 0;
		}
	}

	// effacer une mesure (background)
    public void delRow(long id){
	    Uri uri = ContentUris.withAppendedId(DataProvider.Get_URI(DataProvider.RECIPIENTS), id);
	    if (uri!=null) {
		    ContentResolver content = mContext.getContentResolver();
		    content.delete(uri, null, null);
		    size--;
		    int rank = 0;
		    for (Recipient recipient : getAllRows()) {
			    recipient.setRank(rank);
			    updateRow(recipient);
			    rank++;
		    }
	    }
   }

	// mettre à jour une mesure (background)
    public void updateRow(Recipient row){
        ContentValues values = new ContentValues();
	    values.put(RECIPIENTS_RANK, row.getRank());
	    values.put(RECIPIENTS_PHONE, row.getPhone());
        values.put(RECIPIENTS_NAME, row.getName());
	    values.put(RECIPIENTS_MESSAGE, row.getMessage());
	    values.put(RECIPIENTS_STATUS, row.getStatus());
	    values.put(RECIPIENTS_POSITION, row.getPosition());
	    values.put(RECIPIENTS_CALLING, row.getCalling());
	    Uri uri = ContentUris.withAppendedId(DataProvider.Get_URI(DataProvider.RECIPIENTS), row.getId());
	    ContentResolver content = mContext.getContentResolver();
	    content.update(uri, values, null, null);
    }

	// interrogation de la bdd sur l'UI thread
    public List<Recipient> getAllRows(){
        List<Recipient> rows = new ArrayList<>();
	    // Retrieve records
	    String URL = DataProvider.RECIPIENTS_URL;
	    Uri data = Uri.parse(URL);
	    Cursor cursor = mContext.getContentResolver().query(data, null, null, null, RECIPIENTS_RANK + " ASC");

	    if (cursor != null) {
		    cursor.moveToFirst();
		    while (!cursor.isAfterLast()) {
			    Recipient row = cursorToRow(cursor);
			    rows.add(row);
			    cursor.moveToNext();
		    }
		    cursor.close();
	    }
	    return rows;
    }

	// interrogation de la bdd sur l'UI thread
	public Recipient getRecipientById(long id){
		Recipient recipient;
		// Retrieve record
		String URL = DataProvider.RECIPIENTS_URL + "/" + id;
		Uri data = Uri.parse(URL);
		Cursor cursor = mContext.getContentResolver().query(data, null, null, null, null);
		if (cursor!=null && cursor.getCount()==1){
			cursor.moveToFirst();
			recipient = cursorToRow(cursor);
		} else {
			recipient = null;
		}
		if (cursor != null) {
			cursor.close();
		}
		return recipient;
	}

	// interrogation de la bdd sur l'UI thread
	public Recipient getCurrentRecipientByNum(String num){
		Recipient recipient;
		// Retrieve record
		String URL = DataProvider.RECIPIENTS_URL;
		String where;
		Uri data = Uri.parse(URL);
		where = RECIPIENTS_PHONE + "=\"" + num + "\" " +                                // the specified phone number
				"and " + RECIPIENTS_CALLING + "=1 " +                                   // currently called
				"and (" + RECIPIENTS_STATUS + "=-1 or " + RECIPIENTS_STATUS + "=1)";    // with no answer or just answered 'got'
		Cursor cursor = mContext.getContentResolver().query(data, null, where, null, null);
		if (cursor!=null && cursor.getCount()>0){
			cursor.moveToFirst();
			recipient = cursorToRow(cursor);
		} else {
			recipient = null;
		}
		if (cursor != null) {
			cursor.close();
		}
		return recipient;
	}

	// interrogation de la bdd sur l'UI thread
	public Recipient getRecipientByRank(int rank){
		Recipient recipient;
		// Retrieve record
		String URL = DataProvider.RECIPIENTS_URL;
		Uri data = Uri.parse(URL);
		Cursor cursor = mContext.getContentResolver().query(data, null, RECIPIENTS_RANK+"="+rank, null, null);
		if (cursor!=null && cursor.getCount()==1){
			cursor.moveToFirst();
			recipient = cursorToRow(cursor);
		} else {
			recipient = null;
		}
		if (cursor != null) {
			cursor.close();
		}
		return recipient;
	}

	// transformer le contenu d'un curseur en mesure
    public static Recipient cursorToRow(Cursor cursor){
	    Recipient recipient = new Recipient();
        recipient.setId(cursor.getLong(cursor.getColumnIndex(RECIPIENTS_ID)));
	    recipient.setRank(cursor.getInt(cursor.getColumnIndex(RECIPIENTS_RANK)));
 	    recipient.setPhone(cursor.getString(cursor.getColumnIndex(RECIPIENTS_PHONE)));
	    recipient.setName(cursor.getString(cursor.getColumnIndex(RECIPIENTS_NAME)));
        recipient.setMessage(cursor.getString(cursor.getColumnIndex(RECIPIENTS_MESSAGE)));
	    recipient.setStatus(cursor.getInt(cursor.getColumnIndex(RECIPIENTS_STATUS)));
	    recipient.setPosition(cursor.getInt(cursor.getColumnIndex(RECIPIENTS_POSITION))==1);
	    recipient.setCalling(cursor.getInt(cursor.getColumnIndex(RECIPIENTS_CALLING))==1);
	    return recipient;
    }
	
    public static class Recipient {
	    long id;
	    int rank;
	    String phone;
	    String name;
	    String message;
	    int status;         //-1:indéterminé; 0:indisponible; 1:vu; 2:refus; 3:j'arrive;
	    boolean position;
	    boolean calling;    // are we currently calling him ?

	    public Recipient(){}

	    public long getId(){return id;}
	    public int getRank(){return rank;}
	    public String getPhone(){return phone;}
	    public String getName(){return name;}
	    public String getMessage(){return message;}
	    public int getStatus(){return status;}
	    public boolean getPosition(){return position;}
	    public boolean getCalling(){return calling;}

	    public void setId(long value) {id = value;}
	    public void setRank(int value) {rank = value;}
	    public void setPhone(String value) {phone = formatPhoneNumber(value, true);}
	    public void setName(String value){name = value;}
	    public void setMessage(String value){message = value;}
	    public void setStatus(int value){status = value;}
	    public void setPosition(boolean value){position = value;}
	    public void setCalling(boolean value){calling = value;}
    }
}




