package org.oftx.ipbanplugin;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CountryResponse;
import com.maxmind.geoip2.record.Country;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

public class IPBanPlugin extends JavaPlugin implements Listener {

    private DatabaseReader dbReader;
    private List<String> allowedCountries;
    private String kickMessage;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        try {
            File database = new File(getDataFolder(), "GeoLite2-Country.mmdb");
            dbReader = new DatabaseReader.Builder(database).build();
        } catch (IOException e) {
            getLogger().severe("Could not load GeoIP2 database.");
            e.printStackTrace();
        }
        loadConfiguration();
    }

    @Override
    public void onDisable() {
        if (dbReader != null) {
            try {
                dbReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @EventHandler
    public void onPlayerLogin(PlayerLoginEvent event) {
        String playerName = event.getPlayer().getName();
        InetAddress address = event.getAddress();

        if (event.getPlayer().hasPermission("ipbanplugin.bypass")) {
            getLogger().info(playerName + " has bypass permission, allowing login.");
            return;
        }

        try {
            if (address.equals(InetAddress.getByName("127.0.0.1"))) {
                return;
            }
            CountryResponse response = dbReader.country(address);
            Country country = response.getCountry();
            String countryCode = country.getIsoCode();
            if (!allowedCountries.contains(countryCode)) {
                String ipAddress = address.getHostAddress();
                String countryName = country.getName();
                String formattedKickMessage = kickMessage.replace("%ip%", ipAddress).replace("%country%", countryName);
                event.disallow(PlayerLoginEvent.Result.KICK_OTHER, formattedKickMessage);
                getLogger().info(playerName + " was denied access. IP: " + ipAddress + " Country: " + countryCode);
            }
        } catch (GeoIp2Exception | IOException e) {
            e.printStackTrace();
        }
    }

    private void loadConfiguration() {
        allowedCountries = getConfig().getStringList("allowed-countries");
        kickMessage = getConfig().getString("kick-message", "You are not allowed to join from your location.");
    }
}
