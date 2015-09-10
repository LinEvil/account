package com.mengcraft.account.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.mengcraft.account.entity.lib.SecureUtil;

@Entity
@Table(name = "pre_ucenter_members")
public class User {
	
	@Id
	private int uid;
	
	@Column(unique = true)
	private String username;
	
	@Column
	private String password;
	
	@Column
	private String salt;
	
	@Column
	private String regip;

	public int getUid() {
		return uid;
	}

	public void setUid(int uid) {
		this.uid = uid;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getSalt() {
		return salt;
	}

	public void setSalt(String salt) {
		this.salt = salt;
	}
	
	public boolean valid(String in) {
		SecureUtil util = SecureUtil.DEFAULT;
		try {
			in = util.digest(util.digest(in) + getSalt());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return password.equals(in);
	}
	
	/**
	 * @return {@code true} if is a registered user.
	 */
	public boolean valid() {
		return getUid() != 0;
	}

	public String getRegip() {
		return regip;
	}

	public void setRegip(String regip) {
		this.regip = regip;
	}

}
