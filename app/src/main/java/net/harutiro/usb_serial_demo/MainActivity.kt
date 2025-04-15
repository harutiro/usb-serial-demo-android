package net.harutiro.usb_serial_demo

import android.content.Context
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import net.harutiro.usb_serial_demo.ui.theme.UsbserialdemoTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UsbserialdemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {

    val context = LocalContext.current
    var usbIoManager: SerialInputOutputManager? = null
    var port: UsbSerialPort? = null

    var result by remember { mutableStateOf("") }
    val state = rememberScrollState()


    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        Button(onClick = {
            // Find all available drivers from attached devices.
            val manager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            val availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager)
            if (availableDrivers.isEmpty()) {
                return@Button
            }


            // Open a connection to the first available driver.
            val driver = availableDrivers[0]
            val connection = manager.openDevice(driver.device)
            if (connection == null) {
                // add UsbManager.requestPermission(driver.getDevice(), ..) handling here
                return@Button
            }

            port = driver.ports[0] // Most devices have just one port (port 0)
            port?.open(connection)
            port?.setParameters(3000000, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)

            Log.d("tag",port?.readEndpoint?.maxPacketSize.toString())

        }) {
            Text(
                text = "接続開始",
                modifier = modifier
            )
        }

//        Button(
//            onClick = {
//                val READ_WAIT_MILLIS = 1000
//                val response = ByteArray(256)
//                val len = port?.read(response, READ_WAIT_MILLIS);
//                Log.d("USB", String(response))
//            }
//        ) {
//            Text(
//                text = "一行受信",
//                modifier = modifier
//            )
//        }

        Button(
            onClick = {
                val buffer = StringBuilder()
                var count = 0

                usbIoManager = SerialInputOutputManager(port, object : SerialInputOutputManager.Listener {
                    override fun onRunError(e: Exception) {
                        Log.e("USB", "エラー: ${e.message}")
                    }

                    override fun onNewData(data: ByteArray) {
                        val receivedText = String(data)
                        buffer.append(receivedText)

                        var newlineIndex = buffer.indexOf("\n")
                        while (newlineIndex != -1) {
                            val line = buffer.substring(0, newlineIndex).trimEnd('\r')
                            Log.d("USB", "受信: $line")
                            buffer.delete(0, newlineIndex + 1)
                            newlineIndex = buffer.indexOf("\n")


                            if(count % 100 == 0){
                                result += line + "\n"
                            }
                            count++

                        }
                    }
                })
                usbIoManager?.start()
            }
        ){
            Text(
                text = "イベントの受信開始",
                modifier = modifier
            )
        }

        Button(onClick = {
            usbIoManager?.stop()
            usbIoManager?.listener = null
            usbIoManager = null
            port?.close()
            port = null
        }) {
            Text(
                text = "接続終了",
                modifier = modifier
            )
        }

        Column(
            modifier = Modifier
                .background(Color.LightGray)
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .verticalScroll(state)
        ) {
            Text(
                text = result
            )
        }
    }


}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    UsbserialdemoTheme {
        Greeting("Android")
    }
}