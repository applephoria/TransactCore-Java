package com.transactcore.infra;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Gerenciador de Conexao JDBC com o PostgreSQL 18.
 * Camada: Infraestrutura (infra)
 * 
 * Responsavel por estabelecer a comunicacao fisica via Socket TCP/IP
 * com a instancia do banco de dados relacional.
 * 
 * Autor: Marcel Florentino
 */
public class DatabaseConnection {

    // Anatomia da URL JDBC:
    // jdbc:            -> Protocolo oficial Java para banco de dados
    // postgresql:      -> Sub-protocolo que aciona o driver PostgreSQL baixado via Maven
    // //localhost:     -> Endereco da maquina servidora (127.0.0.1 / maquina local)
    // 5432             -> Porta TCP/IP padrao mundial do PostgreSQL
    // /transactcore_db -> Nome do banco de dados fisico
    // ?currentSchema=  -> Forca o schema corporativo 'financeiro' (proibicao do schema public)
    private static final String URL = "jdbc:postgresql://localhost:5432/transactcore_db?currentSchema=financeiro";

    // Credenciais de acesso local:
    private static final String USER = "postgres";
    private static final String PASSWORD = "waesz@357.";

    /**
     * Metodo fabrica estatico para abertura de sessao fisica com o PostgreSQL.
     * 
     * @return java.sql.Connection sessao ativa de conexao com o banco
     * @throws SQLException caso ocorra falha de rede, porta ou autenticacao
     */
    public static Connection conectar() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
