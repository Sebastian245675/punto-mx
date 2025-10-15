/*
 * Copyright (C) 2022 KriolOS
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.openbravo.pos.data;


import com.openbravo.basic.BasicException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.DirectoryResourceAccessor;

/**
 *
 * @author poolborges
 */
public class DBMigrator {

    private final static Logger LOGGER = Logger.getLogger(DBMigrator.class.getName());
    
    public static void main(String[] args) throws SQLException {
        //execDBMigration();
    }

    public static void execDBMigration(com.openbravo.data.loader.Session dbSession) throws BasicException {
        boolean res = false;
        LOGGER.info("Database Migration init");
        try {
            // Extraer recursos de Liquibase a directorio temporal
            String userHome = System.getProperty("user.home");
            Path tempDir = Paths.get(userHome, "kriolopos", "liquibase-temp");
            Files.createDirectories(tempDir);
            
            // Extraer archivos XML de Liquibase desde classpath
            extractResourceFromClasspath("/pos_liquidbase/db-changelog-master.xml", tempDir);
            extractResourceFromClasspath("/pos_liquidbase/db-changelog-v4_5.xml", tempDir);
            extractResourceFromClasspath("/pos_liquidbase/db-changelog-v4_5__LOAD.xml", tempDir);
            extractResourceFromClasspath("/pos_liquidbase/db-changelog-v5_0.xml", tempDir);
            
            // Extraer archivos de plantillas (templates) necesarios para Liquibase
            String[] templates = {
                "APrinter.FiscalTicket.xml", "Printer.CloseCash.Preview.xml", "Printer.CloseCash.xml",
                "Printer.CustomerPaid.xml", "Printer.CustomerPaid2.xml", "Printer.FiscalTicket.xml",
                "Printer.Inventory.xml", "Printer.OpenDrawer.xml", "Printer.PartialCash.xml",
                "Printer.PartialCash_.xml", "Printer.PrintLastTicket.xml", "Printer.Product.xml",
                "Printer.ReprintTicket.xml", "Printer.Start.xml", "Printer.Ticket.P1.xml",
                "Printer.Ticket.P2.xml", "Printer.Ticket.P3.xml", "Printer.Ticket.P4.xml",
                "Printer.Ticket.P5.xml", "Printer.Ticket.P6.xml", "Printer.Ticket.xml",
                "Printer.Ticket2.xml", "Printer.TicketClose.xml", "Printer.TicketKitchen.xml",
                "Printer.TicketLine.xml", "Printer.TicketNew.xml", "Printer.TicketPreview.xml",
                "Printer.TicketPreview_A4.xml", "Printer.TicketRemote.xml", "Printer.TicketTotal.xml",
                "Printer.Ticket_A4.xml", "Role.Administrator.xml", "Role.Employee.xml",
                "Role.Guest.xml", "Role.Manager.xml", "Ticket.Buttons.xml",
                "Ticket.Line.xml", "Ticket.TicketLineTaxesIncluded.xml"
            };
            
            for (String template : templates) {
                extractResourceFromClasspath("/com/openbravo/pos/templates/" + template, tempDir);
            }
            
            // Extraer archivos de scripts BeanShell (.bs) necesarios para Liquibase
            String[] beanshellScripts = {
                "application.started.bs", "Cash.Close.bs", "customer.created.bs", "customer.deleted.bs",
                "customer.updated.bs", "event.addline.bs", "event.removeline.bs", "event.setline.bs",
                "Menu.Root.bs", "payment.cash.bs", "script.Event.Total.bs", "script.Keyboard.bs",
                "script.linediscount.bs", "script.poshubclient.bs", "script.ReceiptConsolidate.bs",
                "script.Refundit.bs", "script.SendOrder.bs", "script.ServiceCharge.bs",
                "script.SetPerson.bs", "script.StockCurrentAdd.bs", "script.StockCurrentSet.bs",
                "script.totaldiscount.bs", "script.upsell.bs", "Ticket.Change.bs", "Ticket.Close.bs",
                "Ticket.Discount.bs", "Ticket.SetPerson.bs"
            };
            
            for (String script : beanshellScripts) {
                extractResourceFromClasspath("/com/openbravo/pos/templates/" + script, tempDir);
            }
            
            // Extraer archivos .txt en templates
            extractResourceFromClasspath("/com/openbravo/pos/templates/Window.Title.txt", tempDir);
            
            // Extraer todas las imágenes necesarias para Liquibase
            String[] images = {
                ".01.png", ".02.png", ".05.png", ".10.png", ".20.png", ".25.png", ".50.png",
                "1.00.png", "10.00.png", "100.00.png", "1000.00.png", "1downarrow.png", "1downarrow_b.png",
                "1leftarrow.png", "1rightarrow.png", "1uparrow.png", "2.00.png", "20.00.png", "200.00.png",
                "2downarrow.png", "2leftarrow.png", "2rightarrow.png", "2uparrow.png", "5.00.png", "50.00.png",
                "500.00.png", "app_logo_48x48.png", "app_logo_48x48_backup.png", "app_splash_dark.png",
                "ark2.png", "attributes.png", "auxiliary.png", "bank.png", "barcode.png", "bookmark.png",
                "btn0.png", "btn00.png", "btn1.png", "btn2.png", "btn2a.png", "btn3.png", "btn3a.png",
                "btn4.png", "btn4a.png", "btn5.png", "btn5a.png", "btn6.png", "btn6a.png", "btn7.png",
                "btn7a.png", "btn8.png", "btn8a.png", "btn9.png", "btn9a.png", "btnback.png", "btnce.png",
                "btndiv.png", "btndot.png", "btnequals.png", "btnminus.png", "btnmult.png", "btnplus.png",
                "bundle.png", "calculator.png", "camera.png", "cancel.png", "cash.png", "cashdrawer.png",
                "category.png", "ccard.png", "chart.png", "cheque.png", "coffee.png", "configuration.png",
                "customer.png", "customerpay.png", "customer_add_sml.png", "customer_sml.png", "database.png",
                "date.png", "discount.png", "discount_b.png", "display.png", "edit.png", "editdelete.png",
                "editnew.png", "edit_group.png", "edit_group_sm.png", "empty.png", "encrypted.png",
                "exit.png", "fileclose.png", "fileopen.png", "filesave.png", "floors.png", "gohome.png",
                "heart.png", "img.discount.png", "img.discount_b.png", "img.keyboard_32.png",
                "img.posapps.png", "img.ticket_print.png", "import.png", "inbox.png", "info.png",
                "keyboard_32.png", "keyboard_48.png", "kit_print.png", "leaves.png", "location.png",
                "logout.png", "mail24.png", "maintain.png", "menu-left.png", "menu-right.png", "mime.png",
                "mime2.png", "mime3.png", "movetable.png", "new_pos_icon.png", "notes.png", "no_photo.png",
                "null.png", "ok.png", "package.png", "package_big.png", "paperboard960_600.png",
                "password.png", "pay.png", "payments.png", "plugin.png", "printer.png", "printer24.png",
                "printer24_off.png", "printer24_on.png", "products.png", "products24.png", "receive.png",
                "refundit.png", "reload.png", "remote_print.png", "reports.png", "reprint24.png",
                "resources.png", "restaurant_floor.png", "restaurant_floor_sml.png", "roles.png",
                "run_script.png", "sale.png", "saleedit.png", "sales.png", "sale_delete.png",
                "sale_editline.png", "sale_new.png", "sale_pending.png", "sale_split_sml.png", "scale.png",
                "search24.png", "search32.png", "slip.png", "sort_incr.png", "stockdiary.png",
                "stockmaint.png", "subcategory.png", "supplier_sml.png", "tables.png", "ticket_print.png",
                "timer.png", "user.png", "users.png", "user_sml.png", "utilities.png", "viewmag+.png",
                "viewmag-.png", "voucher.png", "wallet.png", "yast_printer.png"
            };
            
            for (String image : images) {
                extractResourceFromClasspath("/com/openbravo/images/" + image, tempDir);
            }
            
            Connection conn = dbSession.getConnection();
            JdbcConnection connliquibase = new JdbcConnection(conn);
            
            Database databse = DatabaseFactory.getInstance().findCorrectDatabaseImplementation(connliquibase);
            
            // Usar el directorio temporal para Liquibase
            DirectoryResourceAccessor resourceAccessor = new DirectoryResourceAccessor(tempDir);
            Liquibase liquibase = new Liquibase("pos_liquidbase/db-changelog-master.xml", resourceAccessor, databse);

            //Run database
            liquibase.update("pos-database-update");
            res = true;
        } catch (DatabaseException ex) {
            LOGGER.log(Level.SEVERE,"DB Migration Exception: " , ex);
            throw new BasicException("DB Migration Exception: ", ex);
        } catch (LiquibaseException | SQLException | IOException ex) {
            LOGGER.log(Level.SEVERE, "DB Migration Exception: ", ex);
            throw new BasicException("DB Migration Exception: ", ex);
        }
    }
    
    private static void extractResourceFromClasspath(String resourcePath, Path targetDir) throws IOException {
        try (InputStream is = DBMigrator.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("No se encontró el recurso: " + resourcePath);
            }
            
            // Crear la estructura de directorios
            String fileName = resourcePath.substring(resourcePath.lastIndexOf('/') + 1);
            String dirPath = resourcePath.substring(1, resourcePath.lastIndexOf('/'));
            Path fullTargetDir = targetDir.resolve(dirPath);
            Files.createDirectories(fullTargetDir);
            
            Path targetFile = fullTargetDir.resolve(fileName);
            Files.copy(is, targetFile, StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("Recurso extraído: " + resourcePath + " -> " + targetFile);
        }
    }
}
