package com.hridayesh.volfix

import android.app.Activity
import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log

class UsbAttachActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d("VolFix", "EarPods plugged in! Invisible Activity started.")

        val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
        val usbManager = getSystemService(Context.USB_SERVICE) as UsbManager

        if (device != null) {
            // Because it's an Activity triggered by the hardware intent, Android grants permission automatically!
            Log.d("VolFix", "Device detected. Applying hardware volume fix...")
            maxVolume(usbManager, device)
        } else {
            Log.e("VolFix", "Error: No USB device passed to intent.")
        }
        
        // Instantly close the activity so you never see it on screen
        finishAndRemoveTask()
    }

    private fun maxVolume(mgr: UsbManager, device: UsbDevice) {
        val conn: UsbDeviceConnection = mgr.openDevice(device) ?: return
        
        // WE DELETED claimInterface() SO WE DON'T KILL ANDROID'S AUDIO DRIVER

        val FU_ID = 0x02
        val IFACE = 0x00
        val CN = 0x00
        val wValue = (0x02 shl 8) or CN
        val wIndex = (FU_ID shl 8) or IFACE

        // GET_RANGE
        val rangeBuf = ByteArray(8)
        conn.controlTransfer(0xA1, 0x02, wValue, wIndex, rangeBuf, rangeBuf.size, 1000)
        val vmax = ((rangeBuf[5].toInt() shl 8) or (rangeBuf[4].toInt() and 0xFF)).toShort()

        // SET_CUR
        val setBuf = byteArrayOf((vmax.toInt() and 0xFF).toByte(), ((vmax.toInt() shr 8) and 0xFF).toByte())
        val result = conn.controlTransfer(0x21, 0x01, wValue, wIndex, setBuf, setBuf.size, 1000)
        
        if (result >= 0) {
            Log.d("VolFix", "SUCCESS: Volume maxed out at DAC value: $vmax")
        } else {
            Log.e("VolFix", "FAILED to write volume command. Kernel blocked it.")
        }
        
        // WE DELETED releaseInterface()
        conn.close()
    }
}