/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rancherclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONException;
import org.json.JSONObject;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;
import org.json.JSONArray;

/**
 *
 * @author gflourenco
 */
public class RancherClient {

  private String rancherBearerToken;
  private String rancherAddress;
  private static String rancherProjects = "/v3/projects/";
  private static String rancherWorkloads = "/workloads/";
  private static String rancherNodes = "/v3/nodes/";

  public RancherClient() {
    this("xxx.xxx.xxx.xxx", "token-xxx");
  }

  public RancherClient(String rancherAddress, String token) {
    this.rancherAddress = rancherAddress;
    this.rancherBearerToken = token;
  }

  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) {

    try {

      RancherClient rancherClient = new RancherClient();
      
      System.out.println("\nProjects");
      JSONArray projects = rancherClient.getProjects();
      for (int i = 0; i < projects.length(); i++) {
        System.out.println(projects.getJSONObject(i).toString());
      }

      System.out.println("\nWorkloads");
      JSONArray workloads = rancherClient.getWorkloads("c-sm5kw:p-lx87n");
      for (int i = 0; i < workloads.length(); i++) {
        System.out.println(workloads.getJSONObject(i).toString());
      }

      System.out.println("\nWorkload");
      JSONObject workload = rancherClient.getWorkload("c-sm5kw:p-lx87n", "daemonset:default:see-teste");
      System.out.println(workload.toString());

      System.out.println("\nNode");
      JSONObject node = rancherClient.getNode("c-sm5kw:m-81dd8018f312");
      System.out.println(node.toString());

      System.out.println("\nWorkload Raw");
      JSONObject workloadRaw = rancherClient.getWorkload("c-sm5kw:p-lx87n", "daemonset:default:see-teste", true);
      System.out.println(workloadRaw.toString());

      System.out.println("\nUpdate Workload");
      JSONObject updateContainer = new JSONObject();

      updateContainer.put("name", "see-teste");
      updateContainer.put("image", "docker.nosinovacao.pt/see:1.13.1");
      JSONObject environment = new JSONObject();
      environment.put("VAR1", "false");
      environment.put("VAR2", "Teste222");
      updateContainer.put("environment", environment);
      //  System.out.println(rancherClient.updateWorkload("c-sm5kw:p-lx87n", "daemonset:default:see-teste", updateContainer).toString());

    } catch (JSONException ex) {
      Logger.getLogger(RancherClient.class.getName()).log(Level.SEVERE, null, ex);
    }

  }

  public JSONObject updateWorkload(String projectId, String workloadId, JSONObject containerUpdt) {
    JSONObject workloadObj = getWorkload(projectId, workloadId, true);

    try {

      for (int i = 0; i < workloadObj.getJSONArray("containers").length(); i++) {

        if (workloadObj.getJSONArray("containers").getJSONObject(i).get("name").equals(containerUpdt.get("name"))) {
          workloadObj.getJSONArray("containers").getJSONObject(i).put("image", containerUpdt.get("image"));
          if (containerUpdt.has("environment")) {
            workloadObj.getJSONArray("containers").getJSONObject(i).put("environment", containerUpdt.get("environment"));
          }

        }
      }
    } catch (JSONException ex) {
      Logger.getLogger(RancherClient.class.getName()).log(Level.SEVERE, null, ex);
    }
    return putDataGetJson("https://" + rancherAddress + rancherProjects + projectId + rancherWorkloads + "/" + workloadId, workloadObj);
  }

  public JSONObject getNode(String nodeID) {
    JSONObject returnObj = new JSONObject();

    try {

      JSONObject workloadObj = readJsonFromUrl("https://" + rancherAddress + rancherNodes + nodeID);
      returnObj.put("id", workloadObj.get("id"));
      returnObj.put("hostname", workloadObj.get("hostname"));
      returnObj.put("externalIpAddress", workloadObj.get("externalIpAddress"));
    } catch (JSONException ex) {
      Logger.getLogger(RancherClient.class.getName()).log(Level.SEVERE, null, ex);
    }
    return returnObj;
  }

  public JSONObject getWorkload(String projectId, String workloadId, boolean raw) {
    if (raw) {
      return readJsonFromUrl("https://" + rancherAddress + rancherProjects + projectId + rancherWorkloads + "/" + workloadId);
    }
    return getWorkload(projectId, workloadId);
  }

  public JSONObject getWorkload(String projectId, String workloadId) {
    JSONObject returnObj = new JSONObject();

    try {

      JSONObject workloadObj = readJsonFromUrl("https://" + rancherAddress + rancherProjects + projectId + rancherWorkloads + "/" + workloadId);

      returnObj.put("name", workloadObj.get("name"));
      returnObj.put("id", workloadObj.get("id"));
      returnObj.put("created", workloadObj.get("created"));
      returnObj.put("createdTS", workloadObj.get("createdTS"));
      returnObj.put("projectId", workloadObj.get("projectId"));
      returnObj.put("daemonSetStatus", workloadObj.get("daemonSetStatus"));

      returnObj.put("state", workloadObj.get("state"));
      if (workloadObj.has("publicEndpoints")) {

        returnObj.put("publicEndpoints", workloadObj.get("publicEndpoints"));

      }
      JSONArray containersArr = workloadObj.getJSONArray("containers");

      JSONArray returnContainersArr = new JSONArray();

      for (int i = 0; i < containersArr.length(); i++) {
        JSONObject containerObj = containersArr.getJSONObject(i);
        JSONObject returnContainerObj = new JSONObject();
        if (containerObj.has("environment")) {
          returnContainerObj.put("environment", containerObj.get("environment"));
        }
        if (containerObj.has("image")) {
          returnContainerObj.put("image", containerObj.get("image"));
        }

        if (containerObj.has("ports")) {
          returnContainerObj.put("ports", containerObj.get("ports"));
        }
        returnContainersArr.put(returnContainerObj);
      }
      returnObj.put("containers", returnContainersArr);
    } catch (JSONException ex) {
      Logger.getLogger(RancherClient.class.getName()).log(Level.SEVERE, null, ex);
    }
    return returnObj;
  }

  public JSONArray getWorkloads(String projectId) {
    JSONArray returnArr = new JSONArray();

    try {
      JSONObject json = readJsonFromUrl("https://" + rancherAddress + rancherProjects + projectId + rancherWorkloads);
      JSONArray dataArr = json.getJSONArray("data");
      for (int i = 0; i < dataArr.length(); i++) {
        JSONObject dataObj = dataArr.getJSONObject(i);
        JSONObject returnObj = new JSONObject();
        returnObj.put("name", dataObj.get("name"));
        returnObj.put("created", dataObj.get("created"));
        returnObj.put("createdTS", dataObj.get("createdTS"));
        returnObj.put("id", dataObj.get("id"));

        returnArr.put(returnObj);
      }
    } catch (JSONException ex) {
      Logger.getLogger(RancherClient.class.getName()).log(Level.SEVERE, null, ex);
    }
    return returnArr;

  }

  public JSONArray getProjects() {
    JSONArray returnArr = new JSONArray();
    try {
      JSONObject json = readJsonFromUrl("https://" + rancherAddress + rancherProjects);

      JSONArray dataArr = json.getJSONArray("data");
      for (int i = 0; i < dataArr.length(); i++) {
        JSONObject dataObj = dataArr.getJSONObject(i);
        JSONObject returnObj = new JSONObject();
        returnObj.put("name", dataObj.get("name"));
        returnObj.put("created", dataObj.get("created"));
        returnObj.put("createdTS", dataObj.get("createdTS"));
        returnObj.put("creatorId", dataObj.get("creatorId"));
        returnObj.put("description", dataObj.get("description"));
        returnObj.put("id", dataObj.get("id"));
        returnArr.put(returnObj);
      }
    } catch (JSONException ex) {
      Logger.getLogger(RancherClient.class.getName()).log(Level.SEVERE, null, ex);
    }
    return returnArr;

  }

  private String readAll(Reader rd) throws IOException {
    StringBuilder sb = new StringBuilder();
    int cp;
    while ((cp = rd.read()) != -1) {
      sb.append((char) cp);
    }
    return sb.toString();
  }

  public JSONObject readJsonFromUrl(String url) {
    try {
      // Create a trust manager that does not validate certificate chains
      TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
          return null;
        }

        public void checkClientTrusted(X509Certificate[] certs, String authType) {
        }

        public void checkServerTrusted(X509Certificate[] certs, String authType) {
        }
      }
      };

      // Install the all-trusting trust manager
      SSLContext sc = SSLContext.getInstance("SSL");
      sc.init(null, trustAllCerts, new java.security.SecureRandom());
      HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

      URLConnection connection = new URL(url).openConnection();
      HttpsURLConnection httpConn = (HttpsURLConnection) connection;
      httpConn.setRequestProperty("Authorization", "Bearer " + rancherBearerToken);

      InputStream is = httpConn.getInputStream();

      try {
        BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
        String jsonText = readAll(rd);
        JSONObject json = new JSONObject(jsonText);
        return json;
      } catch (JSONException ex) {
        Logger.getLogger(RancherClient.class.getName()).log(Level.SEVERE, null, ex);
      } finally {
        is.close();
      }
    } catch (NoSuchAlgorithmException | KeyManagementException | IOException ex) {
      Logger.getLogger(RancherClient.class.getName()).log(Level.SEVERE, null, ex);
    }
    return null;
  }

  public JSONObject putDataGetJson(String url, JSONObject data) {
    try {
      // Create a trust manager that does not validate certificate chains
      TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
          return null;
        }

        public void checkClientTrusted(X509Certificate[] certs, String authType) {
        }

        public void checkServerTrusted(X509Certificate[] certs, String authType) {
        }
      }
      };

      // Install the all-trusting trust manager
      SSLContext sc = SSLContext.getInstance("SSL");
      sc.init(null, trustAllCerts, new java.security.SecureRandom());
      HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

      URLConnection connection = new URL(url).openConnection();
      HttpsURLConnection httpConn = (HttpsURLConnection) connection;

      httpConn.setRequestMethod("PUT");
      httpConn.setDoOutput(true);
      httpConn.setRequestProperty("Authorization", "Bearer " + rancherBearerToken);
      httpConn.setRequestProperty("Accept", "application/json");
      httpConn.setRequestProperty("Content-Type", "application/json");

      OutputStreamWriter osw = new OutputStreamWriter(httpConn.getOutputStream());
      osw.write(data.toString());
      osw.flush();
      osw.close();
      InputStream is = httpConn.getInputStream();

      try {
        BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
        String jsonText = readAll(rd);
        JSONObject json = new JSONObject(jsonText);
        return json;
      } catch (JSONException ex) {
        Logger.getLogger(RancherClient.class.getName()).log(Level.SEVERE, null, ex);
      } finally {
        is.close();
      }
    } catch (NoSuchAlgorithmException ex) {
      Logger.getLogger(RancherClient.class.getName()).log(Level.SEVERE, null, ex);
    } catch (KeyManagementException ex) {
      Logger.getLogger(RancherClient.class.getName()).log(Level.SEVERE, null, ex);
    } catch (MalformedURLException ex) {
      Logger.getLogger(RancherClient.class.getName()).log(Level.SEVERE, null, ex);
    } catch (IOException ex) {
      Logger.getLogger(RancherClient.class.getName()).log(Level.SEVERE, null, ex);
    }
    return null;
  }
}
