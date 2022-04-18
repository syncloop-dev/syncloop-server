package com.eka.middleware.pub.util.Document;

import java.text.DecimalFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Map;
import java.util.regex.Pattern;

import javax.json.JsonObject;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.eka.middleware.service.DataPipeline;
import com.eka.middleware.template.SnippetException;

public class Function {
	public static final Logger LOGGER = LogManager.getLogger(Function.class);
	public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

	public static boolean isRequired(DataPipeline dp, String pointer, Map<String, String> data) throws SnippetException {
		Object obj = dp.getValueByPointer(pointer);
		if(data==null)
			return false;
		Boolean isRequired = Boolean.parseBoolean(data.get("isRequiredField"));
		if (obj == null && isRequired != null && isRequired == true) {
			String description=data.get("fieldDescription");
			if(description!=null && description.length()>0)
				description=new String(Base64.getDecoder().decode(description));
			throw new SnippetException(dp,"Required validation failure", new ValidationException("Required field('" + pointer + "') missing. " + description));
		}
		return isRequired;
	}

	public static void validate(DataPipeline dp, String pointer, String typePath, Map<String, String> data) throws SnippetException {
		Object object = dp.getValueByPointer(pointer);
		boolean required = isRequired(dp, pointer, data);
		if (object == null)
			return;
		
		String type = typePath.substring(typePath.lastIndexOf("/") + 1);

		LOGGER.trace("TypePath: " + typePath);
		LOGGER.trace("Type: " + type);
		String response = null;
		switch (type) {

		case "string": {
			String value = (String) object;
			response = applyStringValidations(value, data);
			break;
		}
		case "integer": {
			int value = (Integer) object;
			response = applyIntegerValidations(value, data);
			break;
		}
		case "number": {
			double value = (Double) object;
			response = applyNumberValidations(dp, pointer, typePath, value, data);
			break;
		}
		case "date": {
			String value = (String) object;
			response = applyDateValidations(dp, pointer, typePath, value, data);
			break;
		}
		default:
			dp.log("Unexpected value: " + type, Level.WARN);
		}
		
		String description=data.get("fieldDescription");
		if(description!=null)
			description=new String(Base64.getDecoder().decode(description));
		
		
		if(required && response!=null) {
			throw new SnippetException(dp,"Validation error", new Exception("Pointer : "+pointer+" :"+ response+" : "+description));
		}else if(response!=null)
			dp.log("Pointer : "+pointer+" :"+ response+" : "+description, Level.WARN);
	}

	public static String applyIntegerValidations(int value, Map<String, String> data) {

		if (data == null)
			return null;

		String minimumInteger = data.get("minimumInteger");
		int minInt = 0;
		if (minimumInteger != null) {
			minInt = Integer.parseInt(minimumInteger.trim());
			if (value < minInt)
				return "Minimum integer value allowed is " + minInt;
		}

		String maximumInteger = data.get("maximumInteger");
		int maxInt = 0;
		if (maximumInteger != null) {
			maxInt = Integer.parseInt(maximumInteger.trim());
			if (value > maxInt)
				return "Maximum integer value allowed is " + maxInt;
		}
		return null;
	}

	public static String applyNumberValidations(DataPipeline dp, String pointer, String typePath, double value,
			Map<String, String> data) {

		if (data == null)
			return null;

		String minimumNumber = data.get("minimumNumber");
		double minDbl = 0;
		if (minimumNumber != null) {
			minDbl = Double.parseDouble(minimumNumber.trim());
			if (value < minDbl)
				return "Minimum integer value allowed is " + minDbl;
		}

		String maximumNumber = data.get("maximumNumber");
		double maxDbl = 0;
		if (maximumNumber != null) {
			maxDbl = Double.parseDouble(maximumNumber.trim());
			if (value < maxDbl)
				return "Maximum integer value allowed is " + maxDbl;
		}

		String decimalFormat = data.get("decimalFormat");
		if (decimalFormat != null && decimalFormat.trim().length() > 1) {
			DecimalFormat df = new DecimalFormat(decimalFormat);
			Double newDouble = Double.parseDouble(df.format(value));
			dp.setValueByPointer(pointer, newDouble, typePath);
		}

		return null;
	}

	public static String applyStringValidations(String value, Map<String, String>  data) {

		if (data == null)
			return null;

		if (value == null || value.length() == 0)
			return null;

		String minLength = data.get("minLength");
		String maxLength = data.get("maxLength");
		String stringValidation = data.get("stringValidation");
		String regexPattern = data.get("regex");
		if (regexPattern != null) {
			regexPattern = new String(Base64.getDecoder().decode(regexPattern));
			if (stringValidation == null)
				stringValidation = "regex";
		}
		if (stringValidation != null && stringValidation.equals("email"))
			regexPattern = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";
		if (stringValidation != null && stringValidation.equals("url"))
			regexPattern = "<^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]>";

		if (regexPattern != null)
			if (!patternMatches(value, regexPattern))
				return stringValidation.toUpperCase() + " validation failed for " + value;

		if (minLength != null) {
			int len = Integer.parseInt(minLength.trim());
			if (value.length() < len)
				return "Length should be greater than equal to " + len;
		}

		if (maxLength != null) {
			int len = Integer.parseInt(maxLength.trim());
			if (value.length() > len)
				return "Length should be less than equal to " + len;
		}

		return null;
	}

	public static String applyDateValidations(DataPipeline dp, String pointer, String typePath, String value,
			Map<String, String>  data) {

		if (data == null)
			return null;

		if (value == null || value.length() == 0)
			return null;

		String dateFormat = data.get("dateFormat");
		String toDateFormat = data.get("toDateFormat");
		String startDate = data.get("startDate");
		String endDate = data.get("endDate");

		if (dateFormat == null)
			return "Date format not provided";
		else {
			DateTimeFormatter dateformatter = DateTimeFormatter.ofPattern(dateFormat);// "yyyy-MM-dd HH:mm:ss z");
			ZonedDateTime inDateTime = null;
			try {
				inDateTime=ZonedDateTime.parse(value.trim(), dateformatter);
			} catch (Exception e) {
				dp.setValueByPointer(pointer, null, typePath);
				return "Datetime format incorrect: "+e.getMessage();
			}
					
			ZonedDateTime startDateTime = null;
			ZonedDateTime endDateTime = null;
			if (startDate != null) {
				startDateTime = ZonedDateTime.parse(startDate, formatter);
				if (inDateTime.isBefore(startDateTime))
					return "Datetime before " + startDateTime + " is not allowed";
			}

			if (endDate != null) {
				endDateTime = ZonedDateTime.parse(endDate, formatter);

				if (startDateTime != null && endDateTime.isAfter(startDateTime))
					return "Start date validation range is not mentioned correctly. End date should not come after start date";

				if (inDateTime.isAfter(endDateTime))
					return "Datetime after " + endDateTime + " is not allowed";
			}

			if (toDateFormat != null) {
				DateTimeFormatter toDateFormatter = DateTimeFormatter.ofPattern(dateFormat);
				String outDate = inDateTime.format(toDateFormatter);
				dp.setValueByPointer(pointer, outDate, typePath);
			}

		}

		return null;
	}

	public static boolean patternMatches(String text, String regexPattern) {
		return Pattern.compile(regexPattern).matcher(text).matches();
	}

}
