package com.saas.billing.adapter.jdbc;

import com.saas.billing.domain.Invoice;
import com.saas.billing.port.InfrastructureException;
import com.saas.billing.port.InvoiceRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;

public class JdbcInvoiceRepository implements InvoiceRepository {

    private static final String INSERT =
            "INSERT INTO invoices(customer_id, amount, status, created_at) VALUES (?, ?, ?, NOW())";

    private final DataSource dataSource;

    public JdbcInvoiceRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void save(Invoice invoice) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(INSERT)) {
            stmt.setString(1, invoice.customerId());
            stmt.setBigDecimal(2, invoice.amount().rounded());
            stmt.setString(3, invoice.status().name());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new InfrastructureException("Erro ao gravar fatura do cliente " + invoice.customerId(), e);
        }
    }
}
