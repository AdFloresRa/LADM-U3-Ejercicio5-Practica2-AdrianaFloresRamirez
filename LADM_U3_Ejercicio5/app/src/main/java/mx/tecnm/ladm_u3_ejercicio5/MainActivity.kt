package mx.tecnm.ladm_u3_ejercicio5

import android.content.ContentValues
import android.content.Intent
import android.database.sqlite.SQLiteException
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main2.*

class MainActivity : AppCompatActivity() {
    //Firebase
    var FIREB = FirebaseFirestore.getInstance();
    //sql local
    var baseDatos = BaseDatos(this,"basedatos1",null,1)

    var listaID = ArrayList<String>()
    var datos = ArrayList<String>()
    var DATA= ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        agregarlista()
        btnInser.setOnClickListener {
            insertar()
        }
        btnSinc.setOnClickListener{
            sincronizar()
        }
        Lista.setOnItemClickListener {adapterView,view, i, l ->mostrarAlertEliminarActualizar(i) }
    }

    //INSERTAR
    private fun insertar(){
        try {
            var b = baseDatos.writableDatabase
            var campo = ContentValues()
            campo.put("DESCRIPCION",EditTextDescripcion.text.toString())
            campo.put("FECHA",EditTextFecha.text.toString())
            campo.put("HORA",EditTextHora.text.toString())
            campo.put("LUGAR",EditTextLugar.text.toString())

            var respuesta =b.insert("EVENTO",null,campo)
            if(respuesta==-1L){
                mensaje("FALLÓ AL INSERTAR")
            }else{
                mensaje("INSERCIÓN EXITOSA")
                limpiarCampos()
            }
            b.close()
        }catch (e: SQLiteException){
            mensaje(e.message!!)
        }
        agregarlista()
    }

    //LIMPIAR CAMPOS
    private fun limpiarCampos(){
        EditTextDescripcion.setText("")
        EditTextFecha.setText("")
        EditTextHora.setText("")
        EditTextLugar.setText("")
    }

    //LA LISTA
    private fun agregarlista(){
        datos.clear()
        listaID.clear()
        try{
            var b = baseDatos.readableDatabase
            var eventos = ArrayList<String>()
            var respuesta = b.query("EVENTO", arrayOf("*"),null,null,null,null,null)
            listaID.clear()
            if (respuesta.moveToFirst()){
                do{
                    var concatenacion = "DESCRIPCION: ${respuesta.getString(1)}\nLUGAR: ${respuesta.getString(2)}\nFECHA :${respuesta.getString(3)}\n" +
                            "HORA :${respuesta.getString(4)}"
                    eventos.add(concatenacion)
                    datos.add(concatenacion)
                    listaID.add(respuesta.getInt(0).toString())
                }while (respuesta.moveToNext())

            }else{
                eventos.add("NO TIENES EVENTOS")
            }
            Lista.adapter = ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,eventos)
            this.registerForContextMenu(Lista)
            b.close()
        }catch (e: SQLiteException){mensaje("ERROR!! "+e.message!!)}
    }

    //MOSTAR MENU ELIMINAR/ACTUALIZAR
    private fun mostrarAlertEliminarActualizar(Posicion:Int){
        var idLista=listaID.get(Posicion)
        AlertDialog.Builder(this)
                .setTitle("ATENCION!!")
                .setMessage("¿Que desea hacer con \n${datos.get(Posicion)}?")
                .setPositiveButton("ELIMINIAR"){d,i->eliminar(idLista)}
                .setNeutralButton("CANCELAR"){d,i->}
                .setNegativeButton("ACTUALIZAR"){d,i->llamarVentanaAcualizar(idLista)}
                .show()
    }

    //MENSAJE
    private fun mensaje(s:String){
        AlertDialog.Builder(this)
                .setTitle("ATENCIÓN!!")
                .setMessage(s)
                .setPositiveButton("OK"){d,i->d.dismiss()}
                .show()
    }

    private fun llamarVentanaAcualizar(idLista:String){
        var ventana= Intent(this,MainActivity2::class.java)
        ventana.putExtra("id",idLista)
        mensaje(idLista)
        startActivity(ventana)
        finish()
    }
    private fun sincronizar() {
        DATA.clear()
        FIREB.collection("evento")
            .addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                if (firebaseFirestoreException != null) {
                    mensaje("ERROR! No se pudo recuperar data desde FireBase")
                    return@addSnapshotListener
                }
                var cadena = ""
                for (registro in querySnapshot!!) {
                    cadena = registro.id.toString()
                    DATA.add(cadena)
                }
                try {
                    var trans = baseDatos.readableDatabase
                    var respuesta =
                        trans.query("EVENTO", arrayOf("*"), null, null, null, null, null)
                    if (respuesta.moveToFirst()) {
                        do {

                            if (DATA.any {
                                    respuesta.getString(0).toString() == it
                                })//////id de la tabla
                            {
                                DATA.remove(respuesta.getString(0).toString())
                                FIREB.collection("evento")
                                    .document(respuesta.getString(0))
                                    .update(
                                        "DESCRIPCION",
                                        respuesta.getString(1),
                                        "LUGAR",
                                        respuesta.getString(2),
                                        "FECHA",
                                        respuesta.getString(3),
                                        "HORA",
                                        respuesta.getString(4)
                                    ).addOnSuccessListener {

                                    }.addOnFailureListener {
                                        AlertDialog.Builder(this)
                                            .setTitle("Error")
                                            .setMessage("NO SE PUDO ACTUALIZAR\n${it.message!!}")
                                            .setPositiveButton("Ok") { d, i -> }
                                            .show()
                                    }
                            } else {
                                var datosInsertar = hashMapOf(
                                    "DESCRIPCION" to respuesta.getString(1),
                                    "FECHA" to respuesta.getString(2),
                                    "HORA" to respuesta.getString(3),
                                    "LUGAR" to respuesta.getString(4)
                                )
                                FIREB.collection("evento")
                                    .document("${respuesta.getString(0)}")
                                    .set(datosInsertar as Any).addOnSuccessListener {

                                    }
                                    .addOnFailureListener {
                                        mensaje("NO SE PUDO INSERTAR:\n${it.message!!}")
                                    }
                            }
                        } while (respuesta.moveToNext())

                    } else {
                        mensaje("NO EVENTOS")
                    }
                    trans.close()
                } catch (e: SQLiteException) {
                    mensaje("ERROR: " + e.message!!)
                }
                var el = DATA.subtract(listaID)
                if (el.isEmpty()) {

                } else {
                    el.forEach {
                        FIREB.collection("evento")
                            .document(it)
                            .delete()
                            .addOnSuccessListener {}
                            .addOnFailureListener { mensaje("ERROR!! No se elimino\n" + it.message!!) }
                    }
                }

            }

        Toast.makeText(this, "Sincronizacion Exitosa :)", Toast.LENGTH_SHORT).show()
    }

        private fun eliminar(idEliminar:String){
        try {
            var b = baseDatos.writableDatabase
            var resultado = b.delete("EVENTO","ID=?",
                    arrayOf(idEliminar))
            if (resultado == 0){
                mensaje("ERROR!! No se pudo eliminar")

            }else{
                mensaje("SE ELIMINÓ CON EXITO ID ${idEliminar}")
                agregarlista()
            }
            b.close()
        }catch (e:SQLiteException){
            mensaje(e.message!!)
        }
    }
}
