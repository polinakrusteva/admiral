/*
 * Copyright (c) 2016 VMware, Inc. All Rights Reserved.
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with separate copyright notices
 * and license terms. Your use of these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package loginout

import (
	"bytes"
	"encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"os"
	"strings"
	"time"

	"admiral/client"
	"admiral/config"
	"admiral/utils"
	"admiral/utils/urlutils"
)

var (
	LoginFailedError            = errors.New("Login failed.")
	TenantOrUrlNotProvidedError = errors.New("Tenant and/or url not provided.")

	successLoginMsg = "Login successful."
)

type LogInOut struct {
	RequestType string `json:"requestType"`
}

func Login(username, password, configUrl string) (string, error) {
	if configUrl != "" {
		config.URL = configUrl
		config.SetProperty("Url", configUrl)
	}
	os.Remove(utils.TokenPath())
	url := urlutils.BuildUrl(urlutils.LoginOut, nil, true)
	login := &LogInOut{
		RequestType: "LOGIN",
	}
	jsonBody, _ := json.Marshal(login)
	req, _ := http.NewRequest("POST", url, bytes.NewBuffer(jsonBody))
	req.SetBasicAuth(strings.TrimSpace(username), strings.TrimSpace(password))
	resp, _, _ := client.ProcessRequest(req)
	if resp == nil {
		return "", LoginFailedError
	}
	token := resp.Header.Get("x-xenon-auth-token")
	if token == "" {
		return "", LoginFailedError
	}

	if utils.Quiet {
		return token, nil
	}

	if utils.Verbose {
		fmt.Printf("%s: %s\n", "x-xenon-aut-token", token)
	}
	utils.MkCliDir()
	tokenFile, err := os.Create(utils.TokenPath())

	utils.CheckBlockingError(err)
	tokenFile.Write([]byte(token))
	tokenFile.Close()
	return successLoginMsg, nil
}

func Logout() (string, error) {
	var err error
	if !utils.IsVraMode {
		err = logoutStandalone()
	}
	if err != nil {
		return "", err
	}
	os.Remove(utils.TokenPath())
	return "Logged out.", nil
}

func logoutStandalone() error {
	logout := &LogInOut{
		RequestType: "LOGOUT",
	}
	jsonBody, _ := json.Marshal(logout)
	url := urlutils.BuildUrl(urlutils.LoginOut, nil, true)
	req, _ := http.NewRequest("POST", url, bytes.NewBuffer(jsonBody))
	_, _, respErr := client.ProcessRequest(req)
	return respErr
}

func GetInfo() string {
	var buffer bytes.Buffer
	token, tokenSource := utils.GetAuthToken()
	if token == "" {
		return "Not logged in, no token found."
	}
	tokenArr := strings.Split(token, ".")
	if len(tokenArr) < 3 || len(tokenArr) > 3 {
		return ""
	}
	type TokenInfo struct {
		Sub string `json:"sub"`
		Exp int64  `json:"exp"`
	}
	ti := &TokenInfo{}
	info, err := base64.RawStdEncoding.DecodeString(tokenArr[1])
	if err != nil {
		return err.Error()
	}
	json.Unmarshal(info, ti)
	user := strings.Replace(ti.Sub, "/core/authz/users/", "", -1)
	expDate := time.Unix(0, ti.Exp*int64(time.Microsecond))
	buffer.WriteString("User: " + user + "\n")
	buffer.WriteString("Expiration Date: " + expDate.Format("2006.01.02 15:04:05") + "\n")
	buffer.WriteString("Token: " + token + "\n")
	buffer.WriteString("Token source: " + strings.Title(tokenSource))
	return buffer.String()
}

func Loginvra(username, password, tenant, urlF string) (string, error) {
	if tenant == "" || urlF == "" {
		return "", TenantOrUrlNotProvidedError
	}
	login := &RequestLoginVRA{
		Username: username,
		Password: password,
		Tenant:   tenant,
	}
	os.Remove(utils.TokenPath())
	if !strings.HasSuffix(urlF, "/container-service/api") {
		config.URL = urlF + "/container-service/api"
		config.SetProperty("Url", config.URL)
	}
	url := urlF + "/identity/api/tokens"

	jsonBody, err := json.Marshal(login)
	utils.CheckBlockingError(err)
	req, _ := http.NewRequest("POST", url, bytes.NewBuffer(jsonBody))
	_, respBody, respErr := client.ProcessRequest(req)
	if respErr != nil {
		return "", respErr
	}
	respLogin := &ResponseLoginVRA{}
	err = json.Unmarshal(respBody, &respLogin)

	if err != nil || respLogin.Id == "" {
		return "", LoginFailedError
	}
	config.SetProperty("Tenant", tenant)

	if !utils.Quiet {
		utils.MkCliDir()
		tokenFile, err := os.Create(utils.TokenPath())
		utils.CheckBlockingError(err)
		tokenFile.Write([]byte("Bearer " + respLogin.Id))
		tokenFile.Close()
	} else {
		return "Bearer " + respLogin.Id, nil
	}

	return successLoginMsg, nil
}

type RequestLoginVRA struct {
	Username string `json:"username"`
	Password string `json:"password"`
	Tenant   string `json:"tenant"`
}

type ResponseLoginVRA struct {
	Expires string `json:"expires"`
	Id      string `json:"id"`
	Tenant  string `json:"tenant"`
}
