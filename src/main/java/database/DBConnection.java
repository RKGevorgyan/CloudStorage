package database;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

/**
 * This static class is getting connection to Database.
 * Connection parameters are in @see data.property.
 */
public class DBConnection {

    /**
     * This static method uses data.property
     * to get connection to Database
     * @return Connection
     * @throws SQLException Can be thrown if connection parameters are incorrect.
     * @throws IOException Can be thrown if file path is incorrect.
     */
    public static Connection getConnection() throws SQLException, IOException {
        Properties props = new Properties();
        String separator = File.separator;
        String path = "src"+separator+"main"+separator+"resources"+separator+"data.properties";
        InputStream in = new FileInputStream(path);
        props.load(in);
        String url = props.getProperty("url");
        String user = props.getProperty("user");
        String password = props.getProperty("password");
        return DriverManager.getConnection(url,user,password);
    }

    public static void insert(String name, String pass){
        try(Connection connection = getConnection()) {
            Statement statement = connection.createStatement();
            statement.execute("Insert into CloudStorage.users (login, password) values ('"+name+"','"+pass+"')");
        } catch (SQLException | IOException exception) {
            exception.printStackTrace();
        }
    }
    public static String select(String name, String pass){
        String query = "Select login, password from CloudStorage.users " +
                "where login = '"+name+"' and password = '"+pass+"'";
        try(Connection connection = getConnection()){
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(query);
            while (resultSet.next()){
                name = resultSet.getString("login");
            }
        } catch (SQLException | IOException e){
            e.printStackTrace();
            System.out.println("Problems with connection");
        }
        return name;
    }

    public static boolean update(String newLogin, String oldLogin){
        boolean flag=false;
        String query = "Update CloudStorage.users set login = '" + newLogin +"' where login = '" + oldLogin+"'";
        try(Connection connection = getConnection()) {
            Statement statement = connection.createStatement();
            statement.executeUpdate(query);
            flag = true;
        } catch (SQLException | IOException exception) {
            exception.printStackTrace();
        }
        return flag;
    }
}
