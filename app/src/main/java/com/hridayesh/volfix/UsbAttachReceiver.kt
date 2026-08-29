import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager

class UsbAttachReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE) ?: return
        val usbManager = ctx.getSystemService(Context.USB_SERVICE) as UsbManager

        if (!usbManager.hasPermission(device)) {
            val pi = PendingIntent.getBroadcast(ctx, 0, Intent("USB_PERM"),
                PendingIntent.FLAG_MUTABLE)
            usbManager.requestPermission(device, pi)
            return
        }
        maxVolume(usbManager, device)
    }

    private fun maxVolume(mgr: UsbManager, device: UsbDevice) {
        val conn: UsbDeviceConnection = mgr.openDevice(device) ?: return
        val acIface = device.getInterface(0)   // Audio Control interface index — verify via descriptor dump
        conn.claimInterface(acIface, true)

        val FU_ID = 0x02
        val IFACE = 0x00
        val CN = 0x00
        val wValue = (0x02 shl 8) or CN
        val wIndex = (FU_ID shl 8) or IFACE

        // GET_RANGE
        val rangeBuf = ByteArray(8)
        conn.controlTransfer(0xA1, 0x02, wValue, wIndex, rangeBuf, rangeBuf.size, 1000)
        val vmax = ((rangeBuf[3].toInt() shl 8) or (rangeBuf[2].toInt() and 0xFF)).toShort()

        // SET_CUR
        val setBuf = byteArrayOf((vmax.toInt() and 0xFF).toByte(), ((vmax.toInt() shr 8) and 0xFF).toByte())
        conn.controlTransfer(0x21, 0x01, wValue, wIndex, setBuf, setBuf.size, 1000)

        conn.releaseInterface(acIface)
        conn.close()
    }
}