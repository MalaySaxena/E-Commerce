package com.example.demo.controllers;

import com.example.demo.TestUtils;
import com.example.demo.model.persistence.Cart;
import com.example.demo.model.persistence.Item;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.UserOrder;
import com.example.demo.model.persistence.repositories.CartRepository;
import com.example.demo.model.persistence.repositories.ItemRepository;
import com.example.demo.model.persistence.repositories.OrderRepository;
import com.example.demo.model.persistence.repositories.UserRepository;
import com.example.demo.model.requests.CreateUserRequest;
import com.example.demo.model.requests.ModifyCartRequest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.ArrayList;
import java.util.List;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ControllerTest {

    private UserController userController;
    private OrderController orderController;
    private CartController cartController;
    private ItemController itemController;
    private OrderRepository orderRepository = mock(OrderRepository.class);
    private UserRepository userRepository = mock(UserRepository.class);
    private CartRepository cartRepository = mock(CartRepository.class);
    private ItemRepository itemRepository = mock(ItemRepository.class);
    private BCryptPasswordEncoder bCryptPasswordEncoder = mock(BCryptPasswordEncoder.class);

    @Before
    public void setup(){
        userController = new UserController();
        itemController = new ItemController();
        orderController = new OrderController();
        cartController = new CartController();

        TestUtils.injectObjects(userController, "userRepository", userRepository);
        TestUtils.injectObjects(userController, "cartRepository", cartRepository);
        TestUtils.injectObjects(userController, "bCryptPasswordEncoder", bCryptPasswordEncoder);

        TestUtils.injectObjects(itemController, "itemRepository", itemRepository);

        TestUtils.injectObjects(orderController, "userRepository", userRepository);
        TestUtils.injectObjects(orderController, "orderRepository", orderRepository);

        TestUtils.injectObjects(cartController, "userRepository", userRepository);
        TestUtils.injectObjects(cartController, "cartRepository", cartRepository);
        TestUtils.injectObjects(cartController, "itemRepository", itemRepository);

    }


    @Test
    public void createUserTest() throws Exception{

        when(bCryptPasswordEncoder.encode("abcabcabc")).thenReturn("hashedPassword");

        CreateUserRequest createUserRequest = new CreateUserRequest();
        createUserRequest.setUsername("Test");
        createUserRequest.setPassword("abcabcabc");
        createUserRequest.setConfirmPassword("abcabcabc");

        final ResponseEntity<User> response = userController.createUser(createUserRequest);

        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());

        User createdUser = response.getBody();

        assertEquals(0, createdUser.getId());
        assertEquals("Test", createdUser.getUsername());
        assertEquals("hashedPassword", createdUser.getPassword());

        createUserRequest.setUsername("Test1");

        userController.createUser(createUserRequest);

        ResponseEntity<User> test1User = userController.findByUserName("Test1");
        User test2User = userController.findByUserName("Test2").getBody();

        //assertEquals("Test1", test1User);
        //assertEquals(200, test1User.getStatusCodeValue());
        assertNull("Test2", test2User);

    }

    @Test
    public void itemControllerTest(){
        Item items = new Item();
        items.setDescription("Toy");
        items.setId(0L);
        items.setName("bicycle");
        items.setPrice(new BigDecimal(50.00));
        List<Item> itemList = new ArrayList<>();
        itemList.add(items);
        when(itemRepository.findById(anyLong())).thenReturn(java.util.Optional.of(items));
        ResponseEntity<Item> responseEntity = itemController.getItemById(0L);
        assertNotNull(responseEntity);
        assertEquals(200, responseEntity.getStatusCodeValue());

        when(itemRepository.findByName(anyString())).thenReturn(itemList);
        ResponseEntity<List<Item>> responseEntityList = itemController.getItemsByName(items.getName());
        assertNotNull(responseEntityList);
        assertEquals(200, responseEntityList.getStatusCodeValue());
    }

    @Test
    public void cartControllerTest(){
        User user = new User();
        user.setUsername("Test");

        Cart cart = new Cart();
        user.setCart(cart);

        Item items = new Item();
        items.setDescription("Toy");
        items.setId(0L);
        items.setName("Toy");
        items.setPrice(new BigDecimal(50.00));

        cart.addItem(items);

        ModifyCartRequest modifyCartRequest = new ModifyCartRequest();
        modifyCartRequest.setItemId(0L);
        modifyCartRequest.setUsername("Test");
        modifyCartRequest.setQuantity(1);

        when(userRepository.findByUsername(anyString())).thenReturn(user);
        when(itemRepository.findById(anyLong())).thenReturn(Optional.of(items));
        ResponseEntity<Cart> responseEntity = cartController.addTocart(modifyCartRequest);
        assertNotNull(responseEntity);
        assertEquals(200, responseEntity.getStatusCodeValue());

        responseEntity = cartController.removeFromcart(modifyCartRequest);
        assertNotNull(responseEntity);
        assertEquals(200, responseEntity.getStatusCodeValue());

    }

    @Test
    public void orderControllerTest(){

        User user = new User();
        user.setUsername("test");
        when(userRepository.findByUsername(anyString())).thenReturn(user);
        Cart cart = new Cart();

        Item items = new Item();
        items.setDescription("Toy");
        items.setId(0L);
        items.setName("Toy");
        items.setPrice(new BigDecimal(50.00));

        cart.addItem(items);
        user.setCart(cart);
        ResponseEntity<UserOrder> userOrderResponseEntity = orderController.submit(user.getUsername());
        assertNotNull(userOrderResponseEntity);
        assertEquals(200, userOrderResponseEntity.getStatusCodeValue());

        ResponseEntity<List<UserOrder>> ordersList = orderController.getOrdersForUser(user.getUsername());

        assertNotNull(ordersList);
        assertEquals(200, ordersList.getStatusCodeValue());

    }
}
