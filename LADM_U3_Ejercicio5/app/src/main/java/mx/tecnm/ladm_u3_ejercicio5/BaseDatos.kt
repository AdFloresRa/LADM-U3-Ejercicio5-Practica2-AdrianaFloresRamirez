package mx.tecnm.ladm_u3_ejercicio5

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import javax.xml.transform.TransformerFactory

class BaseDatos(context: Context?, name:String?, factory:SQLiteDatabase.CursorFactory?, version:Int):SQLiteOpenHelper
    (context,name,factory,version){
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE EVENTO(ID INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, DESCRIPCION VARCHAR(200), FECHA VARCHAR(200),HORA VARCHAR(200),LUGAR VARCHAR(200))")
    }
    override fun onUpgrade(p0: SQLiteDatabase?, p1: Int, p2: Int) {}
}