/*
 * Copyright (C) 2011 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.errai.samples.restdemo.client.local;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.Response;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import org.jboss.errai.common.client.api.Caller;
import org.jboss.errai.common.client.api.RemoteCallback;
import org.jboss.errai.enterprise.client.jaxrs.MarshallingWrapper;
import org.jboss.errai.enterprise.client.jaxrs.api.RestErrorCallback;
import org.jboss.errai.ioc.client.api.EntryPoint;
import org.jboss.errai.samples.restdemo.client.shared.Customer;
import org.jboss.errai.samples.restdemo.client.shared.CustomerNotFoundException;
import org.jboss.errai.samples.restdemo.client.shared.CustomerService;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Example code showing how to use Errai-JAXRS.
 *
 * @author Christian Sadilek <csadilek@redhat.com>
 */
@EntryPoint
public class App {

  @Inject
  private Caller<CustomerService> customerService;

  final private FlexTable customersTable = new FlexTable();
  final private TextBox custFirstName = new TextBox();
  final private TextBox custLastName = new TextBox();
  final private TextBox custPostalCode = new TextBox();

  private final Map<Long, Integer> rows = new HashMap<>();

  private final RemoteCallback<Response> creationCallback = response -> {
    if (response.getStatusCode() == 200) {
      final long id = MarshallingWrapper.fromJSON(response.getText(), Long.class);
      customerService.call((RemoteCallback<Customer>) this::addNewCustomerToTable).retrieveCustomerById(id);
    }
  };

  private final RemoteCallback<Customer> modificationCallback = customer -> {
    addCustomerToTable(customer, rows.get(customer.getId()));
  };

  private final RemoteCallback<Response> deletionCallback = response -> {
    customersTable.removeAllRows();
    populateCustomersTable();
  };

  @PostConstruct
  public void init() {

    final Button createButton = new Button("Create", (ClickHandler) clickEvent -> {
      final Customer customer = new Customer(custFirstName.getText(), custLastName.getText(), custPostalCode.getText());
      customerService.call(creationCallback).createCustomer(customer);
    });

    final Button getSimulatingErrorButton = new Button("Get (Simulate Error)", (ClickHandler) clickEvent -> {
      final RestErrorCallback errorCallback = (message, throwable) -> {
        final CustomerNotFoundException e = (CustomerNotFoundException) throwable;

        final String className = e.getClass().getName();
        final Long customerId = e.getCustomerId();

        Window.alert("As expected, an error of type '" + className + "' was received for ID '" + customerId + "'.");
        return true;
      };

      final RemoteCallback<Customer> successCallback = response -> {
        Window.alert("A customer was returned?  What the what?!");
      };

      customerService.call(successCallback, errorCallback).retrieveCustomerById(nonexistentCustomerId());
    });

    final FlexTable newCustomerTable = new FlexTable();
    newCustomerTable.setWidget(0, 1, custFirstName);
    newCustomerTable.setWidget(0, 2, custLastName);
    newCustomerTable.setWidget(0, 3, custPostalCode);
    newCustomerTable.setWidget(0, 4, createButton);
    newCustomerTable.setStyleName("new-customer-table");

    final VerticalPanel vPanel = new VerticalPanel();
    vPanel.add(customersTable);
    vPanel.add(new HTML("<hr />"));
    vPanel.add(newCustomerTable);
    vPanel.add(new HTML("<hr />"));
    vPanel.add(getSimulatingErrorButton);
    vPanel.addStyleName("whole-customer-table");
    RootPanel.get().add(vPanel);

    populateCustomersTable();
  }

  private long nonexistentCustomerId() {
    return 17L;
  }

  private void populateCustomersTable() {
    customersTable.setText(0, 0, "ID");
    customersTable.setText(0, 1, "First Name");
    customersTable.setText(0, 2, "Last Name");
    customersTable.setText(0, 3, "Postal Code");
    customersTable.setText(0, 4, "Date Changed");

    final RemoteCallback<List<Customer>> listCallback = customers -> {
      customers.forEach(this::addNewCustomerToTable);
    };

    customerService.call(listCallback).listAllCustomers();
  }

  private void addNewCustomerToTable(Customer customer) {
    addCustomerToTable(customer, customersTable.getRowCount() + 1);
  }

  private void addCustomerToTable(final Customer customer, final int row) {
    final TextBox firstName = new TextBox();
    firstName.setText(customer.getFirstName());

    final TextBox lastName = new TextBox();
    lastName.setText(customer.getLastName());

    final TextBox postalCode = new TextBox();
    postalCode.setText(customer.getPostalCode());

    final Button updateButton = new Button("Update", (ClickHandler) clickEvent -> {
      customer.setFirstName(firstName.getText());
      customer.setLastName(lastName.getText());
      customer.setPostalCode(postalCode.getText());
      customerService.call(modificationCallback).updateCustomer(customer.getId(), customer);
    });

    final Button deleteButton = new Button("Delete", (ClickHandler) clickEvent -> {
      customerService.call(deletionCallback).deleteCustomer(customer.getId());
    });

    customersTable.setText(row, 0, Long.toString(customer.getId()));
    customersTable.setWidget(row, 1, firstName);
    customersTable.setWidget(row, 2, lastName);
    customersTable.setWidget(row, 3, postalCode);
    customersTable.setText(row, 4, DateTimeFormat.getFormat("yyyy-MM-dd HH:mm:ss").format(customer.getLastChanged()));
    customersTable.setWidget(row, 5, updateButton);
    customersTable.setWidget(row, 6, deleteButton);

    rows.put(customer.getId(), row);
  }
}
