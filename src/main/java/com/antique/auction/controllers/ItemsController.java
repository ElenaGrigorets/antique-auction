package com.antique.auction.controllers;

import com.antique.auction.models.Item;
import com.antique.auction.models.ItemPrice;
import com.antique.auction.repositories.ItemPriceRepository;
import com.antique.auction.repositories.ItemRepository;
import com.antique.auction.services.ItemsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@RestController
public class ItemsController {
    private final ItemRepository itemRepository;
    private final ItemPriceRepository itemPriceRepository;
    private final ItemsService itemsService;

    @Autowired
    public ItemsController(ItemRepository itemRepository, ItemPriceRepository itemPriceRepository,
                           ItemsService itemsService) {
        this.itemRepository = itemRepository;
        this.itemPriceRepository = itemPriceRepository;
        this.itemsService = itemsService;
    }

    @GetMapping(value = "/items")
    public ModelAndView getItems (ModelAndView modelAndView,
                                @RequestParam("page") Optional<Integer> page,
                                @RequestParam("size") Optional<Integer> size,
                                @RequestParam("param") Optional<String> param,
                                  @RequestParam("sortOrder") Optional<String> sortOrder) {

        return fillModel(modelAndView, page, size, param, sortOrder);
    }

    private ModelAndView fillModel(ModelAndView modelAndView, @RequestParam("page") Optional<Integer> page,
                                   @RequestParam("size") Optional<Integer> size, Optional<String> searchParam, Optional<String> sortOrder) {
        int currentPage = page.orElse(1);
        int pageSize = size.orElse(10);

        Page<Item> itemPage = itemsService.findPaginated(PageRequest.of(currentPage - 1, pageSize), searchParam, sortOrder);
        modelAndView.addObject("itemPage", itemPage);

        int totalPages = itemPage.getTotalPages();
        if (totalPages > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages)
                    .boxed()
                    .collect(Collectors.toList());
            modelAndView.addObject("pageNumbers", pageNumbers);
        }
        modelAndView.setViewName("home");
        return modelAndView;
    }

    @GetMapping(value = "/goToItemsDetail/{id}")
    public ModelAndView gotToItemsDetail (@PathVariable int id) {
        Item item = itemRepository.findById(id).orElse(new Item());
        ModelAndView modelAndView = new ModelAndView("item-details", "item", item);
        return modelAndView;
    }

    @PostMapping(value = "/bid/add/{id}")
    public ModelAndView addBid (@PathVariable int id,  Item item) {
        Item existingItem;
        if (item.getId() != null) {
            existingItem = itemRepository.findById(id).orElse(null);
        } else {
            existingItem = new Item();
        }
        if (item.getCurrentPrice() <= (existingItem != null ? existingItem.getCurrentPrice() : 0)) {
            ModelAndView modelAndView = new ModelAndView("item-details", "item", existingItem);
            modelAndView.addObject("wrongBid", "true");
            return modelAndView;
        }
        existingItem.setBidDate(LocalDateTime.now().plusHours(4));
        existingItem.setCurrentPrice(item.getCurrentPrice());
        ItemPrice currentItemPrice = new ItemPrice();
        currentItemPrice.setItem(existingItem);
        currentItemPrice.setPriceValue(item.getCurrentPrice());
        existingItem.getItemPrices().add(currentItemPrice);
        itemPriceRepository.save(currentItemPrice);
        itemRepository.save(existingItem);
        return fillModel(new ModelAndView(), Optional.of(1), Optional.of(10), Optional.empty(), Optional.empty());
    }

    @RequestMapping(value = {"/", "/home"})
    public ModelAndView home(ModelAndView modelAndView) {
        return fillModel(modelAndView, Optional.of(1), Optional.of(10), Optional.empty(), Optional.empty());
    }

    @RequestMapping("/goToUploadImage")
    public ModelAndView uploadImage(ModelAndView modelAndView) {
        modelAndView.setViewName("upload-image");
        return modelAndView;
    }


    @PostMapping(value = "/item/addEdit/{id}")
    public ModelAndView editItem (@PathVariable int id, Item item) {
        itemRepository.save(item);
        return fillModel(new ModelAndView(), Optional.of(1), Optional.of(10), Optional.empty(), Optional.empty());
    }

    @PostMapping(value = "/item/addEdit")
    public ModelAndView addItem (Item item) {
        itemRepository.save(item);
        return fillModel(new ModelAndView(), Optional.of(1), Optional.of(10), Optional.empty(), Optional.empty());
    }

    @GetMapping(value = "/delete/{id}")
    public ModelAndView addItem (@PathVariable Integer id) {
        itemRepository.deleteById(id);
        return fillModel(new ModelAndView(), Optional.of(1), Optional.of(10), Optional.empty(), Optional.empty());
    }

    @GetMapping("/goToAddEDitItem/{id}")
    public ModelAndView goToAddEDitItem(@PathVariable Integer id) {
        ModelAndView modelAndView = new ModelAndView();
            Item existingItem = itemRepository.findById(id).orElse(null);
            modelAndView.addObject("item", existingItem);
        modelAndView.setViewName("add-edit-item");
        return modelAndView;
    }

    @GetMapping("/goToAddEDitItem")
    public ModelAndView goToAddItem() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject(new Item());
        modelAndView.setViewName("add-edit-item");
            return modelAndView;
    }

    @PostMapping(value = "/image")
//    @ResponseBody
    public ModelAndView getImage(@PathVariable(value = "file") String imageName) throws IOException {

        File serverFile = new File("/static" + imageName + ".jpg");

        Files.readAllBytes(serverFile.toPath());
        return fillModel(new ModelAndView(), Optional.of(1), Optional.of(10), Optional.empty(), Optional.empty());
    }

}
