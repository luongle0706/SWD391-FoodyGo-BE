package com.foodygo.service;

import com.foodygo.dto.CustomerDTO;
import com.foodygo.dto.request.CustomerCreateRequest;
import com.foodygo.dto.request.CustomerUpdateRequest;
import com.foodygo.entity.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CustomerService extends BaseService<Customer, Integer> {
    List<Customer> getAllCustomersActive();

    CustomerDTO undeleteCustomer(Integer customerID);

    User getUserByCustomerID(Integer customerID);

    Customer createCustomer(CustomerCreateRequest customerCreateRequest);

    Customer updateCustomer(CustomerUpdateRequest customerUpdateRequest, int customerID);

    List<Order> getOrdersByCustomerID(Integer customerID);

    Customer getCustomerByOrderID(Integer orderID);

    Building getBuildingByCustomerID(int customerID);

    User getUserByCustomerID(int customerID);

    Wallet getWalletByCustomerID(Integer customerID);

    Customer getCustomerByWalletID(Integer walletID);

//    List<Deposit> getDepositByCustomerID(Integer customerID);
//
//    Customer getCustomerByDepositID(Integer depositID);

}
