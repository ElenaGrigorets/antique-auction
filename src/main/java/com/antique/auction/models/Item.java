package com.antique.auction.models;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
public class Item {

    @Id
    @GeneratedValue
    private Integer id;
    private String name;
    private String description;
    private Integer currentPrice;
    private String bidUserLogin;
    private LocalDateTime currentBidDate;
    private String dateString;
    private String imageName;
    private boolean awarded;

    @OneToMany(mappedBy = "item")
    private List<Bid> bids = new ArrayList<>();

    @ManyToMany(mappedBy = "items", fetch = FetchType.EAGER)
    private List<User> users = new ArrayList<>();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(Integer currentPrice) {
        this.currentPrice = currentPrice;
    }

    public List<Bid> getBids() {
        return bids;
    }

    public String getDateString() {
        return dateString;
    }

    public void setDateString(String dateString) {
        this.dateString = dateString;
    }

    public String getImageName() {
        return imageName;
    }

    public void setImageName(String imageName) {
        this.imageName = imageName;
    }

    public List<User> getUsers() {
        return users;
    }

    public void addUser(User user) {
        users.add(user);
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public boolean isAwarded() {
        return awarded;
    }

    public void setAwarded(boolean awarded) {
        this.awarded = awarded;
    }

    public String getBidUserLogin() {
        return bidUserLogin;
    }

    public void setBidUserLogin(String bidUserLogin) {
        this.bidUserLogin = bidUserLogin;
    }

    public LocalDateTime getCurrentBidDate() {
        return currentBidDate;
    }

    public void setCurrentBidDate(LocalDateTime currentBidDate) {
        this.currentBidDate = currentBidDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Item item = (Item) o;

        return Objects.equals(id, item.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}
