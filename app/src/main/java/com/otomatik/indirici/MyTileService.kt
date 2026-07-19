package com.otomatik.indirici

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

class MyTileService : TileService() {

    // Tile durum çubuğunda görünür hale geldiğinde (ikon, etiket ayarlanır)
    override fun onStartListening() {
        super.onStartListening()
        qsTile.icon = Icon.createWithResource(this, R.drawable.ic_tile)
        qsTile.state = Tile.STATE_ACTIVE
        qsTile.updateTile()
    }

    // Butona tıklandığında çalışacak olan fonksiyon
    override fun onClick() {
        super.onClick()

        // Şimdilik test için ekrana bir mesaj bastıralım
        Toast.makeText(applicationContext, "Butona basıldı! Servis tetikleniyor...", Toast.LENGTH_SHORT).show()

        // TODO: Burada Erişilebilirlik Servisine (Accessibility) sinyal gönderip linki yakalayacağız.
    }

    // Buton durum çubuğuna eklendiğinde
    override fun onTileAdded() {
        super.onTileAdded()
        qsTile.label = "Videoyu İndir"
        qsTile.updateTile()
    }
}
