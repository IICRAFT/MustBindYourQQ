package com.willzcode;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.io.BukkitObjectInputStream;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.*;

/**
 * Created by willz on 2018/3/21.
 */
public class MustBindYourQQ extends JavaPlugin implements Listener {
    Map<String, String> pendingMap = new HashMap<>();
    ServerSocket server;
    public static MustBindYourQQ plugin;
    Set<UUID> lockPlayer = new HashSet<>();
    String groupid = "597884379";//597884379//319200291
    int port = 6811;


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender.isOp() && args.length == 2) {
            String name = args[0];
            String qq = args[1];
            if (!isNumeric(qq)) {
                sender.sendMessage("请输入正确的qq！");
                return true;
            }
            setBindedQQ(name, qq);
            sender.sendMessage("绑定成功");
            return true;
        }

        if(sender instanceof Player)
        if (args.length > 0) {
            String qq = args[0];
            if (!isNumeric(qq)) {
                sender.sendMessage("请输入正确的qq！");
                return true;
            }
            String name = ((Player)sender).getName();
            if (!getBindedQQ(name).isEmpty()) {
                sender.sendMessage("你已绑定qq，暂时不支持重新绑定！");
                return true;
            }

            if (isQQBinded(qq)) {
                sender.sendMessage("这个qq已绑定其他账号，不能再申请绑定了！");
                return true;
            }

            if (checkMember(qq)) {
                pendingMap.put(name, qq);
                sender.sendMessage("已发送请求，请在群里进行验证!");
                sendToGroup("[CQ:at,qq%3d" + qq + "]您的游戏账号["+name+"]在服务器中申请了绑定QQ，请在群里回复下面的文字来绑定：%26bind "+name);
            } else {
                sender.sendMessage("你的qq不在群里，请先加群："+groupid+"!");
            }
        } else {
            sender.sendMessage("绑定指令用法：/bind <QQ号>\n如/bind 10086");
        }
        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCMD(PlayerCommandPreprocessEvent event) {
        String command = event.getMessage().replaceFirst("/", "").toLowerCase();
        if (command.startsWith("pro")) {
            String name = event.getPlayer().getName();
            if (!isPlayerBinded(name)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if(lockPlayer.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            //event.getPlayer().sendMessage("您尚未绑定qq，绑定后才能继续体验游戏，指令/bind <QQ号>");
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if(lockPlayer.contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
//            event.getPlayer().sendMessage("§6您尚未绑定qq，绑定后才能继续体验游戏，指令/bind <QQ号>");
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("绑定qq插件已关闭!");
    }

    @Override
    public void onEnable() {
        plugin = this;

        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getServer().getScheduler().runTaskTimer(this, new Runnable() {
            @Override
            public void run() {
                for (Player p : getServer().getOnlinePlayers()) {
                    PlayerData data = new PlayerData(p.getUniqueId());
                    if (data.get().isEmpty()) {
                        p.sendMessage("§6您尚未绑定qq，绑定后才能继续体验游戏，指令/bind <QQ号>");
                        if(!lockPlayer.contains(p.getUniqueId()))
                            lockPlayer.add(p.getUniqueId());
                    } else {
                        String qq = data.get();
                        if (checkMember(qq)) {
                            if (lockPlayer.contains(p.getUniqueId()))
                                lockPlayer.remove(p.getUniqueId());
                        } else {
                            p.sendMessage("§6绑定的qq[" + qq + "]已不在群里，请加群后再继续体验游戏！群号："+groupid);
                            if(!lockPlayer.contains(p.getUniqueId()))
                                lockPlayer.add(p.getUniqueId());
                        }

                    }
                }

            }
        }, 20L*60L, 20L*60L);

        try
        {
            server = new ServerSocket(port);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        new Thread(){
            @Override
            public void run() {

                while (true)
                {
                    try
                    {
                        Socket sk = server.accept();
                        PrintWriter wtr = new PrintWriter(sk.getOutputStream());
                        BufferedReader rdr = new BufferedReader(new InputStreamReader(sk
                                .getInputStream()));
                        String msg = rdr.readLine();
                        String[] arg = msg.split("\\|");
                        if (arg.length < 2) {
                            throw new StringIndexOutOfBoundsException();
                        }
                        String qq = arg[0];
                        String name = arg[1];
                        if (pendingMap.containsKey(name) && pendingMap.get(name).equals(qq)) {
                            setBindedQQ(name, qq);
                            pendingMap.remove(name);
                            sendToGroup("[CQ:at,qq%3d" + qq + "]绑定成功，请不要随意退群哦！");
                            Bukkit.getPlayer(name).sendMessage("绑定成功，请不要随意退群哦！");
                            if(lockPlayer.contains(Bukkit.getPlayer(name).getUniqueId()))
                                lockPlayer.remove(Bukkit.getPlayer(name).getUniqueId());
                        } else {
                            sendToGroup("[CQ:at,qq%3d" + qq + "]请在游戏中输入指令\"/bind <QQ号>\"来绑定");
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                        //sendToGroup("绑定过程出现异常！");
                    }

                }
            }
        }.start();
        getLogger().info("绑定qq插件已开启!");
    }

    public boolean isQQBinded(String qq) {
        QQData data = new QQData(qq);
        return !data.getName().isEmpty();
    }

    public boolean isPlayerBinded(String name) {
        return !getBindedQQ(name).isEmpty();
    }

    public String getBindedQQ(String name) {
        PlayerData data = new PlayerData(name);
        return data.get();
    }

    public String getBindedPlayer(String qq) {
        QQData data = new QQData(qq);
        return data.getName();
    }

    private void setBindedQQ(String name, String qq) {
        PlayerData data = new PlayerData(name);
        data.set(qq);
        data.save();
        QQData qqData = new QQData(qq);
        qqData.setName(name);
        qqData.save();
    }

    static class PlayerData {
        private UUID uuid;
        private File datafile;
        private YamlConfiguration datayml;

        private final String PLAYER_FOLDER = "Players";
        private final String KEY_BIND_QQ = "binded-qq";

        public PlayerData(UUID uuid) {
            this.uuid = uuid;
            init();
        }

        public PlayerData(String name) {
            this.uuid = plugin.getServer().getOfflinePlayer(name).getUniqueId();
            init();
        }

        private void init() {
            datafile = new File(plugin.getDataFolder().getAbsolutePath() + "/"+PLAYER_FOLDER+"/", uuid + ".yml");
            File dir = new File(plugin.getDataFolder(), PLAYER_FOLDER);

            if(!dir.exists()) {
                dir.mkdir();
            }

            if(!datafile.exists()) {
                try {
                    datafile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            datayml = YamlConfiguration.loadConfiguration(datafile);
        }

        public void save() {
            try {
                datayml.save(datafile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void set(String qq) {
            datayml.set(KEY_BIND_QQ, qq);
        }

        public String get() {
            return datayml.getString(KEY_BIND_QQ, "");
        }
    }

    static class QQData {
        private String qq;
        private File datafile;
        private YamlConfiguration datayml;

        private final String QQ_FOLDER = "QQ";
        private final String KEY_BIND_NAME = "bind-user";

        public QQData(String qq) {
            this.qq = qq;
            datafile = new File(plugin.getDataFolder().getAbsolutePath() + "/"+QQ_FOLDER+"/", qq + ".yml");
            File dir = new File(plugin.getDataFolder(), QQ_FOLDER);

            if(!dir.exists()) {
                dir.mkdir();
            }

            if(!datafile.exists()) {
                try {
                    datafile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            datayml = YamlConfiguration.loadConfiguration(datafile);
        }

        public void save() {
            try {
                datayml.save(datafile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void setName(String name) {
            datayml.set(KEY_BIND_NAME, name);
        }

        public String getName() {
            return datayml.getString(KEY_BIND_NAME, "");
        }
    }

    private String sendToGroup(String msg) {
        return post("http://localhost:5701/send_group_msg?group_id="+groupid, "message=" + msg);
    }

    private boolean isNumeric(String str){
        for (int i = str.length(); --i >= 0; ) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean checkMember(String qq) {
        String rs = requestFromCQ("/get_group_member_info?group_id="+groupid+"&no_cache=1&user_id="+qq);
        JsonParser parser = new JsonParser();
        JsonElement json = parser.parse(rs);
        return !json.getAsJsonObject().get("data").isJsonNull();
    }

    private String getGroupMemberList() {
        return requestFromCQ("/get_group_member_list?group_id="+groupid);
    }

    private String requestFromCQ(String path) {
        return httpGet("http://localhost:5701"+path);
    }

    /**
     * 发起http请求获取返回结果
     * @param req_url 请求地址
     * @return
     */
    private String httpGet(String req_url) {
        StringBuffer buffer = new StringBuffer();
        try {
            URL url = new URL(req_url);
            HttpURLConnection httpUrlConn = (HttpURLConnection) url.openConnection();

            httpUrlConn.setDoOutput(false);
            httpUrlConn.setDoInput(true);
            httpUrlConn.setUseCaches(false);

            httpUrlConn.setRequestMethod("GET");
            httpUrlConn.connect();

            // 将返回的输入流转换成字符串
            InputStream inputStream = httpUrlConn.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "utf-8");
            Reader in = new BufferedReader(inputStreamReader);

            for (int c; (c = in.read()) >= 0; )
                buffer.append((char) c);

            inputStreamReader.close();
            // 释放资源
            inputStream.close();
            inputStream = null;
            httpUrlConn.disconnect();

        } catch (Exception e) {
            System.out.println(e.getStackTrace());
        }
        return buffer.toString();
    }

    /**
     * 发送HttpPost请求
     *
     * @param strURL
     *            服务地址
     * @param params
     *
     * @return 成功:返回json字符串<br/>
     */
    public static String post(String strURL, String params) {
        String response = "";
        try {
            //访问准备
            URL url = new URL(strURL);

            //开始访问
            byte[] postDataBytes = params.toString().getBytes("UTF-8");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
            conn.setDoOutput(true);
            conn.getOutputStream().write(postDataBytes);

            Reader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

            StringBuilder sb = new StringBuilder();
            for (int c; (c = in.read()) >= 0; )
                sb.append((char) c);
            response = sb.toString();
            System.out.println(response);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }
}
