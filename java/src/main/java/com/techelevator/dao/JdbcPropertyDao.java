package com.techelevator.dao;

import com.techelevator.exception.DaoException;
import com.techelevator.model.Amenities;
import com.techelevator.model.Images;
import com.techelevator.model.Property;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class JdbcPropertyDao implements PropertyDAO {

    //instance variables
    @Autowired
    private PropertyDAO propertyDAO;

    private JdbcTemplate jdbcTemplate;

    //constructor
    public JdbcPropertyDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    //GET methods

    // POV: prop own/mgr logs in to view multiple props
    @Override
    public List<Property> getProperties() {
        List<Property> listofMultiProps = new ArrayList<>();
        String sql = "SELECT prop_id, address, city, owner_id, state, zip, vacancy, pending, rent, bedrooms, bathrooms, dishwasher, central_air, laundry, pets_allowed, string_agg(img_url, '|')\n" +
                "FROM properties JOIN images USING(prop_id) JOIN amenities USING(prop_id) JOIN users ON users.user_id = properties.owner_id\n" +
                "GROUP BY prop_id, dishwasher, central_air, laundry, pets_allowed";
        try {
            SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sql);

            while (rowSet.next()) {
                listofMultiProps.add(mapRowToProperty(rowSet));
            }
        } catch (CannotGetJdbcConnectionException e) {
            throw new DaoException("Unable to connect to server or database", e);
        } catch (NullPointerException e){
            throw new DaoException("Property not found.", e);
        }

        return listofMultiProps;
    }

    //POV: prop own/mgr login for specific prop
    @Override
    public List<Property> getPropertiesByOwnerId(int ownerId) {
        List<Property> listofOwnedProps = new ArrayList<>();
        String sql = "SELECT prop_id, address, city, owner_id, state, zip, vacancy, pending, rent, bedrooms, bathrooms, dishwasher, central_air, laundry, pets_allowed, string_agg(img_url, '|')\n" +
                "FROM properties JOIN images USING(prop_id) JOIN amenities USING(prop_id) JOIN users ON users.user_id = properties.owner_id\n" +
                "GROUP BY prop_id, dishwasher, central_air, laundry, pets_allowed" +
                "\"WHERE owner_id = ?";
        try {
            SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sql, ownerId);

            while (rowSet.next()) {
                listofOwnedProps.add(mapRowToProperty(rowSet));
            }
        } catch (CannotGetJdbcConnectionException e) {
            throw new DaoException("Unable to connect to server or database", e);
        } catch (NullPointerException e) {
            throw new DaoException("Property not found. :(", e);
        }

        return listofOwnedProps;
    }

    //POV: specific property for general user login
    @Override
    public Property getPropertyByPropId(int propId) {
        Property oneProp = null;
        String sql = "SELECT prop_id, address, city, owner_id, state, zip, vacancy, pending, rent, bedrooms, bathrooms, dishwasher, central_air, laundry, pets_allowed, string_agg(img_url, '\\|')\n" +
                " FROM properties JOIN images USING(prop_id) JOIN amenities USING(prop_id) JOIN users ON users.user_id = properties.owner_id " +
                " GROUP BY prop_id, dishwasher, central_air, laundry, pets_allowed" +
                " WHERE prop_id = ? ;";
        try {
            SqlRowSet rowSet = jdbcTemplate.queryForRowSet(sql, propId);
            if (rowSet.next()) {
                oneProp = mapRowToProperty(rowSet);
            }
        } catch (CannotGetJdbcConnectionException e) {
            throw new DaoException("Unable to connect to server or database", e);
        } catch (NullPointerException e) {
            throw new DaoException("Property not found. :(", e);
        }

        return oneProp;
    }

    //POST methods

    @Override
    public Property createProperty(Property property, Amenities amenities, Images images) {

        String sql = "INSERT INTO properties (owner_id, address, city, state, zip, vacancy, pending, rent, bedrooms, bathrooms)\n" +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)\n" +
        "RETURNING prop_id";

        String sql2 =
        "INSERT INTO amenities (prop_id, dishwasher, central_air, laundry, pets_allowed)\n" +
        "VALUES (?, ?, ?, ?, ?)";

        String sql3 =
                "INSERT INTO images (prop_id, img_url) VALUES (?,?)";

        try {
            int newPropId =
                    jdbcTemplate.queryForObject(sql, int.class,
                    property.getOwnerId(),
                    property.getAddress(),
                    property.getCity(),
                    property.getState(),
                    property.getZipCode(),
                    property.isVacancy(),
                    property.isPending(),
                    property.getRent(),
                    property.getBedrooms(),
                    property.getBathrooms());

            jdbcTemplate.update(sql2,
                    newPropId,
                    amenities.isDishwasher(),
                    amenities.isCentralAir(),
                    amenities.isLaundry(),
                    amenities.isPetsAllowed());

            jdbcTemplate.update(sql3,
                    newPropId,
                    images.getImageURL());


//            newProperty = getPropertyByPropId(newPropId);
            property.setPropId(newPropId);
        } catch (CannotGetJdbcConnectionException e) {
            throw new DaoException("Unable to connect to server or database", e);
        } catch (NullPointerException e) {
            throw new DaoException("Property not found. :(", e);
        }

        return property;

    }


//    PUT methods
//    prop mgr auth reqd to update prop listings
//    @Override
//    public Property updatePropByPropId(Property property,int propId){
//        Property updatedProp = null;
//
//        String sql = "UPDATE properties\n" +
//                     "SET first_name = ?, last_name = ? \n" +
//                     "WHERE prop_id = ?;";
//
//
//        //    public Lease updateLeaseStatus(Lease lease) {
//        //        String sql =
//        //            "UPDATE leases\n" +
//        //            "SET lease_status = ?\n" +
//        //            "WHERE lease_id = ?\n" +
//        //            ";";
//        //        Lease updatedLease = lease;
//        //        try {
//        //            jdbcTemplate.update(sql,
//        //                    lease.getLeaseStatus());
//        //
//        //        } catch (CannotGetJdbcConnectionException e) {
//        //            throw new DaoException("Unable to connect to server or database", e);
//        //        } catch (NullPointerException e) {
//        //            throw new DaoException("Lease cannot be created.", e);
//        //        }
//        //        return updatedLease;
//        //    }
//
//
//        return updatedProp;
//    }

    //mapRowSet
    private Property mapRowToProperty(SqlRowSet rowSet) {
        Property property = new Property();
        property.setPropId(rowSet.getInt("prop_id"));
        property.setOwnerId(rowSet.getInt("owner_id"));
        property.setAddress(rowSet.getString("address"));
        property.setCity(rowSet.getString("city"));
        property.setState(rowSet.getString("state"));
        property.setZipCode(rowSet.getInt("zip"));
        property.setVacancy(rowSet.getBoolean("vacancy"));
        property.setPending((rowSet.getBoolean("pending")));
        property.setRent(rowSet.getDouble("rent"));
        property.setBedrooms(rowSet.getInt("bedrooms"));
        property.setBathrooms(rowSet.getDouble("bathrooms"));
        if (rowSet.getString("string_agg") != null){
            property.setImgString(rowSet.getString("string_agg").split("\\|"));
        }

        return property;
    }
}


